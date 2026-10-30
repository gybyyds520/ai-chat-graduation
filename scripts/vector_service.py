# -*- coding: utf-8 -*-
"""
本地向量检索 sidecar（免费/开源：ChromaDB BSD + Ollama MIT）。

职责：
  - 接收 Java 后端 KnowledgeBaseService 发来的分块，调用 Ollama(nomic-embed-text) 生成向量，
    存入 Chroma 嵌入式持久库（ops/chroma_data）。
  - 接收查询，对 query 生成向量，按 user_id + document_id 过滤做余弦相似度 top-k 召回。
  - 完全离线、零外部付费依赖；Ollama 不可用/Chroma 异常时返回明确错误码，由 Java 侧降级到 2-gram。

端点：
  GET  /health                 -> {status, chroma, ollama, model}
  POST /ingest                 -> {ingested: n}
       body: {userId, documentId, chunks:[{chunkId, chunkIndex, content}]}
  POST /query                  -> {results:[{chunkId, chunkIndex, score, content}], count}
       body: {userId, documentId, query, topK}
  DELETE /collection?userId=&documentId=  按文档删除该文档全部向量（删除文档时同步）

启动： python scripts/vector_service.py  （默认 127.0.0.1:11435）
"""
import json
import os
import sys
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import chromadb

HOST = os.environ.get("VECTOR_HOST", "127.0.0.1")
PORT = int(os.environ.get("VECTOR_PORT", "11435"))
OLLAMA_URL = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434")
EMBED_MODEL = os.environ.get("EMBED_MODEL", "nomic-embed-text")
DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ops", "chroma_data")
COLLECTION = "knowledge_chunks"
MEMORY_COLLECTION = "ai_memory"

os.makedirs(DATA_DIR, exist_ok=True)
_client = chromadb.PersistentClient(path=DATA_DIR)
_coll = _client.get_or_create_collection(name=COLLECTION, metadata={"hnsw:space": "cosine"})
# 市场级升级：AI 记忆语义召回独立集合（按 user_id 过滤）
_mem_coll = _client.get_or_create_collection(name=MEMORY_COLLECTION, metadata={"hnsw:space": "cosine"})


def ollama_embed(text: str):
    """调用 Ollama /api/embeddings，返回 list[float]。"""
    payload = json.dumps({"model": EMBED_MODEL, "prompt": text}).encode("utf-8")
    req = urllib.request.Request(OLLAMA_URL + "/api/embeddings", data=payload,
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    emb = data.get("embedding")
    if not emb:
        raise RuntimeError("ollama embedding empty")
    return emb


def _where(uid: int, did: int):
    """Chroma 1.x 多字段过滤需显式 $and 组合。"""
    return {"$and": [{"user_id": uid}, {"document_id": did}]}


def ollama_alive() -> bool:
    try:
        req = urllib.request.Request(OLLAMA_URL + "/api/tags")
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status == 200
    except Exception:
        return False


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_json(self):
        n = int(self.headers.get("Content-Length", "0"))
        if n == 0:
            return {}
        raw = self.rfile.read(n).decode("utf-8")
        return json.loads(raw)

    def log_message(self, fmt, *args):
        sys.stderr.write("[vector] " + (fmt % args) + "\n")

    def do_GET(self):
        if self.path.startswith("/health"):
            self._send(200, {
                "status": "ok",
                "chroma": "ok",
                "ollama": ollama_alive(),
                "model": EMBED_MODEL,
                "collection": COLLECTION,
                "count": _coll.count(),
            })
        else:
            self._send(404, {"error": "not found"})

    def do_DELETE(self):
        # /collection?userId=1&documentId=2
        from urllib.parse import urlparse, parse_qs
        qs = parse_qs(urlparse(self.path).query)
        uid = qs.get("userId", [None])[0]
        did = qs.get("documentId", [None])[0]
        if uid is None or did is None:
            self._send(400, {"error": "userId & documentId required"})
            return
        try:
            _coll.delete(where=_where(uid, did))
            self._send(200, {"deleted": True})
        except Exception as e:
            self._send(500, {"error": str(e)})

    def do_POST(self):
        try:
            body = self._read_json()
        except Exception as e:
            self._send(400, {"error": "bad json: %s" % e})
            return

        if self.path.startswith("/ingest"):
            try:
                uid = int(body["userId"])
                did = int(body["documentId"])
                chunks = body.get("chunks", [])
                # 先删旧向量（同文档重传），再写新
                _coll.delete(where=_where(uid, did))
                if not chunks:
                    self._send(200, {"ingested": 0})
                    return
                ids, docs, metas, embs = [], [], [], []
                for c in chunks:
                    content = c.get("content", "")
                    if not content.strip():
                        continue
                    emb = ollama_embed(content)
                    cid = str(c.get("chunkId"))
                    ids.append(cid)
                    docs.append(content)
                    metas.append({"user_id": uid, "document_id": did,
                                  "chunk_index": int(c.get("chunkIndex", 0))})
                    embs.append(emb)
                if ids:
                    _coll.add(ids=ids, documents=docs, metadatas=metas, embeddings=embs)
                self._send(200, {"ingested": len(ids)})
            except Exception as e:
                self._send(500, {"error": "ingest failed: %s" % e})

        elif self.path.startswith("/query"):
            try:
                uid = int(body["userId"])
                did = int(body["documentId"])
                query = body["query"]
                top_k = int(body.get("topK", 5))
                qemb = ollama_embed(query)
                res = _coll.query(
                    query_embeddings=[qemb],
                    n_results=top_k,
                    where=_where(uid, did),
                )
                out = []
                ids = (res.get("ids") or [[]])[0]
                docs = (res.get("documents") or [[]])[0]
                metas = (res.get("metadatas") or [[]])[0]
                dists = (res.get("distances") or [[]])[0]
                for i, cid in enumerate(ids):
                    dist = float(dists[i]) if i < len(dists) else 1.0
                    # cosine distance -> similarity
                    score = max(0.0, 1.0 - dist)
                    out.append({
                        "chunkId": cid,
                        "chunkIndex": int(metas[i].get("chunk_index", 0)) if i < len(metas) else 0,
                        "score": round(score, 6),
                        "content": docs[i] if i < len(docs) else "",
                    })
                self._send(200, {"results": out, "count": len(out)})
            except Exception as e:
                self._send(500, {"error": "query failed: %s" % e})

        elif self.path.startswith("/memory/ingest"):
            # 单条记忆向量化入库：body {userId, memoryId, content}
            try:
                uid = int(body["userId"])
                mid = str(body["memoryId"])
                content = body.get("content", "")
                if not content.strip():
                    self._send(200, {"ingested": 0})
                    return
                emb = ollama_embed(content)
                try:
                    _mem_coll.delete(ids=[mid])
                except Exception:
                    pass
                _mem_coll.add(ids=[mid], documents=[content],
                              metadatas=[{"user_id": uid}], embeddings=[emb])
                self._send(200, {"ingested": 1})
            except Exception as e:
                self._send(500, {"error": "memory ingest failed: %s" % e})

        elif self.path.startswith("/memory/query"):
            # 记忆语义召回：body {userId, query, topK}
            try:
                uid = int(body["userId"])
                query = body["query"]
                top_k = int(body.get("topK", 5))
                qemb = ollama_embed(query)
                res = _mem_coll.query(
                    query_embeddings=[qemb],
                    n_results=top_k,
                    where={"user_id": uid},
                )
                out = []
                ids = (res.get("ids") or [[]])[0]
                docs = (res.get("documents") or [[]])[0]
                dists = (res.get("distances") or [[]])[0]
                for i, mid in enumerate(ids):
                    dist = float(dists[i]) if i < len(dists) else 1.0
                    score = max(0.0, 1.0 - dist)
                    out.append({
                        "memoryId": mid,
                        "score": round(score, 6),
                        "content": docs[i] if i < len(docs) else "",
                    })
                self._send(200, {"results": out, "count": len(out)})
            except Exception as e:
                self._send(500, {"error": "memory query failed: %s" % e})
        else:
            self._send(404, {"error": "not found"})


if __name__ == "__main__":
    print("vector_service starting on http://%s:%d  model=%s  data=%s" % (HOST, PORT, EMBED_MODEL, DATA_DIR))
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
