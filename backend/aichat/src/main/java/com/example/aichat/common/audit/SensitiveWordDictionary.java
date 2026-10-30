package com.example.aichat.common.audit;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置敏感词字典。
 *
 * <p>第一版使用代码常量（不依赖数据库初始化，便于纯单元测试），同时预留
 * {@link #register(String, String, String)} 扩展点，后续可从
 * {@code sensitive_word} 表加载词库后动态合并。</p>
 *
 * <p>分类：porn(色情) / violence(暴力) / politics(政治敏感) / abuse(辱骂) /
 * ads(广告营销) / scam(诈骗) / gambling(赌博) / drugs(毒品) / illegal(违法)。
 * 风险等级 HIGH > MEDIUM > LOW，取命中词的最高等级作为整体风险等级。</p>
 *
 * <p>性能：所有词条统一小写后放入 {@link HashSet}（{@link #WORD_INDEX}），
 * 成员判断 O(1)；审核匹配仍按词条做大小写不敏感的子串包含匹配。</p>
 *
 * <p>未成年加强词表 {@link #MINOR_EXTRA}：仅当用户开启未成年保护模式时额外命中，
 * 普通用户正常放行，实现"未成年过滤更严格"。</p>
 */
@Component
public class SensitiveWordDictionary {

    /** 词条：词、分类、风险等级。 */
    public record WordEntry(String word, String category, String riskLevel) {
    }

    public static final String RISK_LOW = "LOW";
    public static final String RISK_MEDIUM = "MEDIUM";
    public static final String RISK_HIGH = "HIGH";

    /**
     * 内置词库（不可变）。Stage-1 P0：由 23 个扩展至 500+，分九大类。
     * 原有 23 个词条全部保留（既有测试依赖），其余为本次新增。
     */
    private static final List<WordEntry> BUILT_IN = List.of(
            // ===== 色情类（HIGH）=====
            new WordEntry("色情网站", "porn", RISK_HIGH),
            new WordEntry("裸聊", "porn", RISK_HIGH),
            new WordEntry("成人视频", "porn", RISK_HIGH),
            new WordEntry("涉黄信息", "porn", RISK_HIGH),
            new WordEntry("黄色小说", "porn", RISK_HIGH),
            new WordEntry("黄色图片", "porn", RISK_HIGH),
            new WordEntry("色情图片", "porn", RISK_HIGH),
            new WordEntry("色情视频", "porn", RISK_HIGH),
            new WordEntry("色情表演", "porn", RISK_HIGH),
            new WordEntry("裸体写真", "porn", RISK_HIGH),
            new WordEntry("裸体照片", "porn", RISK_HIGH),
            new WordEntry("成人网站", "porn", RISK_HIGH),
            new WordEntry("成人内容", "porn", RISK_HIGH),
            new WordEntry("成人电影", "porn", RISK_HIGH),
            new WordEntry("成人漫画", "porn", RISK_HIGH),
            new WordEntry("情色小说", "porn", RISK_HIGH),
            new WordEntry("情色图片", "porn", RISK_HIGH),
            new WordEntry("性服务", "porn", RISK_HIGH),
            new WordEntry("性交易", "porn", RISK_HIGH),
            new WordEntry("卖淫嫖娼", "porn", RISK_HIGH),
            new WordEntry("招嫖信息", "porn", RISK_HIGH),
            new WordEntry("包养信息", "porn", RISK_HIGH),
            new WordEntry("一夜情", "porn", RISK_HIGH),
            new WordEntry("约炮软件", "porn", RISK_HIGH),
            new WordEntry("援交信息", "porn", RISK_HIGH),
            new WordEntry("在线色情", "porn", RISK_HIGH),
            new WordEntry("黄色网站", "porn", RISK_HIGH),
            new WordEntry("色情直播", "porn", RISK_HIGH),
            new WordEntry("裸聊视频", "porn", RISK_HIGH),
            new WordEntry("色情聊天室", "porn", RISK_HIGH),
            new WordEntry("成人直播", "porn", RISK_HIGH),
            new WordEntry("情色直播", "porn", RISK_HIGH),
            new WordEntry("卖淫信息", "porn", RISK_HIGH),
            new WordEntry("嫖娼信息", "porn", RISK_HIGH),
            new WordEntry("色情短信", "porn", RISK_HIGH),
            new WordEntry("色情游戏", "porn", RISK_HIGH),
            new WordEntry("色情APP", "porn", RISK_HIGH),
            new WordEntry("黄色影片", "porn", RISK_HIGH),
            new WordEntry("色情光碟", "porn", RISK_HIGH),
            new WordEntry("迷奸药", "porn", RISK_HIGH),
            new WordEntry("春药销售", "porn", RISK_HIGH),
            new WordEntry("壮阳药", "porn", RISK_HIGH),
            new WordEntry("色情按摩", "porn", RISK_HIGH),
            new WordEntry("色情场所", "porn", RISK_HIGH),
            new WordEntry("外围女", "porn", RISK_HIGH),
            new WordEntry("伴游服务", "porn", RISK_HIGH),
            new WordEntry("上门服务", "porn", RISK_HIGH),
            new WordEntry("特种服务", "porn", RISK_HIGH),
            new WordEntry("空姐兼职", "porn", RISK_HIGH),
            new WordEntry("学生妹兼职", "porn", RISK_HIGH),
            new WordEntry("白领兼职", "porn", RISK_HIGH),
            new WordEntry("同城约炮", "porn", RISK_HIGH),
            new WordEntry("附近约炮", "porn", RISK_HIGH),
            new WordEntry("成人交友网", "porn", RISK_HIGH),
            new WordEntry("偷拍视频", "porn", RISK_HIGH),
            new WordEntry("偷拍照片", "porn", RISK_HIGH),
            new WordEntry("换妻俱乐部", "porn", RISK_HIGH),
            new WordEntry("多人运动视频", "porn", RISK_HIGH),

            // ===== 暴力类（HIGH）=====
            new WordEntry("暴力伤人", "violence", RISK_HIGH),
            new WordEntry("血腥暴力", "violence", RISK_HIGH),
            new WordEntry("恐怖袭击", "violence", RISK_HIGH),
            new WordEntry("杀人视频", "violence", RISK_HIGH),
            new WordEntry("杀人教程", "violence", RISK_HIGH),
            new WordEntry("分尸视频", "violence", RISK_HIGH),
            new WordEntry("虐杀动物", "violence", RISK_HIGH),
            new WordEntry("虐童视频", "violence", RISK_HIGH),
            new WordEntry("校园暴力", "violence", RISK_HIGH),
            new WordEntry("校园欺凌", "violence", RISK_HIGH),
            new WordEntry("殴打视频", "violence", RISK_HIGH),
            new WordEntry("群架斗殴", "violence", RISK_HIGH),
            new WordEntry("持刀伤人", "violence", RISK_HIGH),
            new WordEntry("持枪伤人", "violence", RISK_HIGH),
            new WordEntry("制造爆炸", "violence", RISK_HIGH),
            new WordEntry("制作炸弹", "violence", RISK_HIGH),
            new WordEntry("土制炸弹", "violence", RISK_HIGH),
            new WordEntry("炸药配方", "violence", RISK_HIGH),
            new WordEntry("枪支买卖", "violence", RISK_HIGH),
            new WordEntry("私藏枪支", "violence", RISK_HIGH),
            new WordEntry("贩卖枪支", "violence", RISK_HIGH),
            new WordEntry("砍人视频", "violence", RISK_HIGH),
            new WordEntry("纵火焚烧", "violence", RISK_HIGH),
            new WordEntry("投放危险物质", "violence", RISK_HIGH),
            new WordEntry("下毒杀人", "violence", RISK_HIGH),
            new WordEntry("绑架勒索", "violence", RISK_HIGH),
            new WordEntry("非法拘禁", "violence", RISK_HIGH),
            new WordEntry("故意伤害他人", "violence", RISK_HIGH),
            new WordEntry("故意杀人", "violence", RISK_HIGH),
            new WordEntry("报复社会", "violence", RISK_HIGH),
            new WordEntry("持刀砍人", "violence", RISK_HIGH),
            new WordEntry("血腥视频", "violence", RISK_HIGH),
            new WordEntry("血腥图片", "violence", RISK_HIGH),
            new WordEntry("杀人直播", "violence", RISK_HIGH),
            new WordEntry("自杀直播", "violence", RISK_HIGH),
            new WordEntry("自残直播", "violence", RISK_HIGH),
            new WordEntry("虐待儿童", "violence", RISK_HIGH),
            new WordEntry("虐待老人", "violence", RISK_HIGH),
            new WordEntry("家庭暴力教学", "violence", RISK_HIGH),
            new WordEntry("威胁恐吓信", "violence", RISK_HIGH),
            new WordEntry("敲诈勒索信", "violence", RISK_HIGH),
            new WordEntry("打手雇佣", "violence", RISK_HIGH),
            new WordEntry("职业杀手", "violence", RISK_HIGH),
            new WordEntry("买凶杀人", "violence", RISK_HIGH),
            new WordEntry("雇凶伤人", "violence", RISK_HIGH),
            new WordEntry("爆炸物制造", "violence", RISK_HIGH),
            new WordEntry("仿真枪买卖", "violence", RISK_HIGH),
            new WordEntry("弹药买卖", "violence", RISK_HIGH),
            new WordEntry("间谍器材", "violence", RISK_HIGH),
            new WordEntry("窃听器材", "violence", RISK_HIGH),
            new WordEntry("跟踪器材", "violence", RISK_HIGH),

            // ===== 政治敏感类（HIGH，仅保留通用违规描述，不枚举具体敏感实体）=====
            new WordEntry("违规政治词汇", "politics", RISK_HIGH),
            new WordEntry("反动宣传", "politics", RISK_HIGH),
            new WordEntry("反动标语", "politics", RISK_HIGH),
            new WordEntry("非法集会煽动", "politics", RISK_HIGH),
            new WordEntry("颠覆国家政权", "politics", RISK_HIGH),
            new WordEntry("分裂国家言论", "politics", RISK_HIGH),
            new WordEntry("仇恨言论煽动", "politics", RISK_HIGH),
            new WordEntry("极端思想传播", "politics", RISK_HIGH),
            new WordEntry("邪教宣传", "politics", RISK_HIGH),
            new WordEntry("邪教组织招募", "politics", RISK_HIGH),

            // ===== 赌博类（HIGH）=====
            new WordEntry("赌博网站", "gambling", RISK_HIGH),
            new WordEntry("在线赌博", "gambling", RISK_HIGH),
            new WordEntry("网络赌博", "gambling", RISK_HIGH),
            new WordEntry("现金赌博", "gambling", RISK_HIGH),
            new WordEntry("体育博彩", "gambling", RISK_HIGH),
            new WordEntry("足球博彩", "gambling", RISK_HIGH),
            new WordEntry("篮球博彩", "gambling", RISK_HIGH),
            new WordEntry("赌场在线", "gambling", RISK_HIGH),
            new WordEntry("真人赌场", "gambling", RISK_HIGH),
            new WordEntry("地下六合彩", "gambling", RISK_HIGH),
            new WordEntry("彩票预测内幕", "gambling", RISK_HIGH),
            new WordEntry("外围赌球", "gambling", RISK_HIGH),
            new WordEntry("赌球网站", "gambling", RISK_HIGH),
            new WordEntry("赌球平台", "gambling", RISK_HIGH),
            new WordEntry("网赌平台", "gambling", RISK_HIGH),
            new WordEntry("赌博APP", "gambling", RISK_HIGH),
            new WordEntry("赌博外挂", "gambling", RISK_HIGH),
            new WordEntry("赌博代充", "gambling", RISK_HIGH),
            new WordEntry("赌博充值", "gambling", RISK_HIGH),
            new WordEntry("赌博套现", "gambling", RISK_HIGH),
            new WordEntry("洗码赌博", "gambling", RISK_HIGH),
            new WordEntry("代理赌博", "gambling", RISK_HIGH),
            new WordEntry("赌博推广", "gambling", RISK_HIGH),
            new WordEntry("赌博招代理", "gambling", RISK_HIGH),
            new WordEntry("澳门赌博网", "gambling", RISK_HIGH),
            new WordEntry("电玩城赌博", "gambling", RISK_HIGH),
            new WordEntry("捕鱼机赌博", "gambling", RISK_HIGH),
            new WordEntry("老虎机在线", "gambling", RISK_HIGH),
            new WordEntry("现金网赌", "gambling", RISK_HIGH),
            new WordEntry("博彩网站", "gambling", RISK_HIGH),
            new WordEntry("博彩代理", "gambling", RISK_HIGH),
            new WordEntry("博彩推广", "gambling", RISK_HIGH),
            new WordEntry("微信赌博群", "gambling", RISK_HIGH),
            new WordEntry("QQ赌博群", "gambling", RISK_HIGH),
            new WordEntry("棋牌赌钱", "gambling", RISK_HIGH),
            new WordEntry("麻将外挂", "gambling", RISK_HIGH),
            new WordEntry("扑克外挂", "gambling", RISK_HIGH),
            new WordEntry("刷流水赌博", "gambling", RISK_HIGH),
            new WordEntry("套利赌博", "gambling", RISK_HIGH),
            new WordEntry("赌博返水", "gambling", RISK_HIGH),
            new WordEntry("赌博包赢", "gambling", RISK_HIGH),
            new WordEntry("赌博稳赚", "gambling", RISK_HIGH),
            new WordEntry("赌博公式", "gambling", RISK_HIGH),

            // ===== 毒品类（HIGH）=====
            new WordEntry("毒品交易", "drugs", RISK_HIGH),
            new WordEntry("冰毒买卖", "drugs", RISK_HIGH),
            new WordEntry("海洛因", "drugs", RISK_HIGH),
            new WordEntry("摇头丸", "drugs", RISK_HIGH),
            new WordEntry("K粉销售", "drugs", RISK_HIGH),
            new WordEntry("大麻种植", "drugs", RISK_HIGH),
            new WordEntry("大麻贩卖", "drugs", RISK_HIGH),
            new WordEntry("迷幻药", "drugs", RISK_HIGH),
            new WordEntry("致幻剂", "drugs", RISK_HIGH),
            new WordEntry("可卡因", "drugs", RISK_HIGH),
            new WordEntry("吗啡销售", "drugs", RISK_HIGH),
            new WordEntry("杜冷丁滥用", "drugs", RISK_HIGH),
            new WordEntry("三唑仑", "drugs", RISK_HIGH),
            new WordEntry("听话水", "drugs", RISK_HIGH),
            new WordEntry("迷奸水", "drugs", RISK_HIGH),
            new WordEntry("失忆水", "drugs", RISK_HIGH),
            new WordEntry("笑气贩卖", "drugs", RISK_HIGH),
            new WordEntry("笑气弹", "drugs", RISK_HIGH),
            new WordEntry("LSD邮票", "drugs", RISK_HIGH),
            new WordEntry("恰特草", "drugs", RISK_HIGH),
            new WordEntry("止咳水滥用", "drugs", RISK_HIGH),
            new WordEntry("联邦止咳露", "drugs", RISK_HIGH),
            new WordEntry("Rush贩卖", "drugs", RISK_HIGH),
            new WordEntry("0号胶囊", "drugs", RISK_HIGH),
            new WordEntry("天那水滥用", "drugs", RISK_HIGH),
            new WordEntry("电子烟毒品", "drugs", RISK_HIGH),
            new WordEntry("大麻烟油", "drugs", RISK_HIGH),
            new WordEntry("大麻种子", "drugs", RISK_HIGH),
            new WordEntry("大麻种植技术", "drugs", RISK_HIGH),
            new WordEntry("冰毒制作", "drugs", RISK_HIGH),
            new WordEntry("冰毒配方", "drugs", RISK_HIGH),
            new WordEntry("合成毒品", "drugs", RISK_HIGH),
            new WordEntry("神仙水", "drugs", RISK_HIGH),
            new WordEntry("开心水", "drugs", RISK_HIGH),
            new WordEntry("奶茶毒品", "drugs", RISK_HIGH),
            new WordEntry("毒品代购", "drugs", RISK_HIGH),
            new WordEntry("毒品快递", "drugs", RISK_HIGH),
            new WordEntry("贩毒群", "drugs", RISK_HIGH),
            new WordEntry("吸毒工具", "drugs", RISK_HIGH),
            new WordEntry("管制药品销售", "drugs", RISK_HIGH),
            new WordEntry("精神药品滥用", "drugs", RISK_HIGH),
            new WordEntry("处方药滥用销售", "drugs", RISK_HIGH),

            // ===== 违法类（HIGH）=====
            new WordEntry("走私违禁品", "illegal", RISK_HIGH),
            new WordEntry("走私香烟", "illegal", RISK_HIGH),
            new WordEntry("走私汽车", "illegal", RISK_HIGH),
            new WordEntry("走私文物", "illegal", RISK_HIGH),
            new WordEntry("象牙买卖", "illegal", RISK_HIGH),
            new WordEntry("犀牛角买卖", "illegal", RISK_HIGH),
            new WordEntry("穿山甲鳞片", "illegal", RISK_HIGH),
            new WordEntry("野生动物交易", "illegal", RISK_HIGH),
            new WordEntry("保护动物买卖", "illegal", RISK_HIGH),
            new WordEntry("电鱼设备", "illegal", RISK_HIGH),
            new WordEntry("毒鱼药", "illegal", RISK_HIGH),
            new WordEntry("非法捕猎", "illegal", RISK_HIGH),
            new WordEntry("枪支配件", "illegal", RISK_HIGH),
            new WordEntry("消音器", "illegal", RISK_HIGH),
            new WordEntry("管制刀具", "illegal", RISK_HIGH),
            new WordEntry("弹簧刀", "illegal", RISK_HIGH),
            new WordEntry("电棍销售", "illegal", RISK_HIGH),
            new WordEntry("电击器", "illegal", RISK_HIGH),
            new WordEntry("催泪喷雾", "illegal", RISK_HIGH),
            new WordEntry("弩销售", "illegal", RISK_HIGH),
            new WordEntry("开锁工具销售", "illegal", RISK_HIGH),
            new WordEntry("万能钥匙", "illegal", RISK_HIGH),
            new WordEntry("撬锁工具", "illegal", RISK_HIGH),
            new WordEntry("翻墙软件", "illegal", RISK_HIGH),
            new WordEntry("VPN购买", "illegal", RISK_HIGH),
            new WordEntry("翻墙加速器", "illegal", RISK_HIGH),
            new WordEntry("黑客教程", "illegal", RISK_HIGH),
            new WordEntry("黑客工具包", "illegal", RISK_HIGH),
            new WordEntry("木马程序销售", "illegal", RISK_HIGH),
            new WordEntry("钓鱼网站制作", "illegal", RISK_HIGH),
            new WordEntry("钓鱼邮件群发", "illegal", RISK_HIGH),
            new WordEntry("个人信息买卖", "illegal", RISK_HIGH),
            new WordEntry("手机号买卖", "illegal", RISK_HIGH),
            new WordEntry("身份证买卖", "illegal", RISK_HIGH),
            new WordEntry("银行卡四件套", "illegal", RISK_HIGH),
            new WordEntry("实名账号买卖", "illegal", RISK_HIGH),
            new WordEntry("微信实名账号", "illegal", RISK_HIGH),
            new WordEntry("抖音账号买卖", "illegal", RISK_HIGH),
            new WordEntry("代办营业执照", "illegal", RISK_HIGH),
            new WordEntry("虚开增值税发票", "illegal", RISK_HIGH),
            new WordEntry("买卖发票", "illegal", RISK_HIGH),
            new WordEntry("假证制作", "illegal", RISK_HIGH),
            new WordEntry("假身份证", "illegal", RISK_HIGH),
            new WordEntry("假毕业证", "illegal", RISK_HIGH),
            new WordEntry("假驾驶证", "illegal", RISK_HIGH),
            new WordEntry("私刻公章", "illegal", RISK_HIGH),

            // ===== 诈骗类（HIGH）=====
            new WordEntry("诈骗转账", "scam", RISK_HIGH),
            new WordEntry("杀猪盘", "scam", RISK_HIGH),
            new WordEntry("网络诈骗", "scam", RISK_HIGH),
            new WordEntry("电信诈骗", "scam", RISK_HIGH),
            new WordEntry("冒充客服", "scam", RISK_HIGH),
            new WordEntry("冒充公检法", "scam", RISK_HIGH),
            new WordEntry("冒充领导", "scam", RISK_HIGH),
            new WordEntry("中奖诈骗", "scam", RISK_HIGH),
            new WordEntry("快递理赔诈骗", "scam", RISK_HIGH),
            new WordEntry("退款诈骗", "scam", RISK_HIGH),
            new WordEntry("网贷诈骗", "scam", RISK_HIGH),
            new WordEntry("校园贷诈骗", "scam", RISK_HIGH),
            new WordEntry("套路贷", "scam", RISK_HIGH),
            new WordEntry("无抵押贷款", "scam", RISK_HIGH),
            new WordEntry("秒批贷款", "scam", RISK_HIGH),
            new WordEntry("身份证贷款", "scam", RISK_HIGH),
            new WordEntry("征信修复", "scam", RISK_HIGH),
            new WordEntry("征信洗白", "scam", RISK_HIGH),
            new WordEntry("银行卡贩卖", "scam", RISK_HIGH),
            new WordEntry("跑分洗钱", "scam", RISK_HIGH),
            new WordEntry("代收代付", "scam", RISK_HIGH),
            new WordEntry("转账洗钱", "scam", RISK_HIGH),
            new WordEntry("虚拟币洗钱", "scam", RISK_HIGH),
            new WordEntry("USDT洗钱", "scam", RISK_HIGH),
            new WordEntry("虚拟币诈骗", "scam", RISK_HIGH),
            new WordEntry("区块链诈骗", "scam", RISK_HIGH),
            new WordEntry("空气币", "scam", RISK_HIGH),
            new WordEntry("传销币", "scam", RISK_HIGH),
            new WordEntry("资金盘", "scam", RISK_HIGH),
            new WordEntry("庞氏骗局", "scam", RISK_HIGH),
            new WordEntry("集资诈骗", "scam", RISK_HIGH),
            new WordEntry("非法集资", "scam", RISK_HIGH),
            new WordEntry("养老投资诈骗", "scam", RISK_HIGH),
            new WordEntry("保健品诈骗", "scam", RISK_HIGH),
            new WordEntry("股票群诈骗", "scam", RISK_HIGH),
            new WordEntry("虚假投资平台", "scam", RISK_HIGH),
            new WordEntry("博彩套利诈骗", "scam", RISK_HIGH),
            new WordEntry("兼职刷单诈骗", "scam", RISK_HIGH),
            new WordEntry("游戏币诈骗", "scam", RISK_HIGH),
            new WordEntry("游戏账号交易诈骗", "scam", RISK_HIGH),
            new WordEntry("花呗套现", "scam", RISK_HIGH),
            new WordEntry("白条套现", "scam", RISK_HIGH),
            new WordEntry("信用卡套现", "scam", RISK_HIGH),

            // ===== 辱骂类（MEDIUM）=====
            new WordEntry("辱骂他人", "abuse", RISK_MEDIUM),
            new WordEntry("王八蛋", "abuse", RISK_MEDIUM),
            new WordEntry("废物东西", "abuse", RISK_MEDIUM),
            new WordEntry("垃圾人", "abuse", RISK_MEDIUM),
            new WordEntry("傻逼", "abuse", RISK_MEDIUM),
            new WordEntry("脑残", "abuse", RISK_MEDIUM),
            new WordEntry("智障", "abuse", RISK_MEDIUM),
            new WordEntry("弱智", "abuse", RISK_MEDIUM),
            new WordEntry("神经病", "abuse", RISK_MEDIUM),
            new WordEntry("疯狗", "abuse", RISK_MEDIUM),
            new WordEntry("贱货", "abuse", RISK_MEDIUM),
            new WordEntry("贱人", "abuse", RISK_MEDIUM),
            new WordEntry("狗杂种", "abuse", RISK_MEDIUM),
            new WordEntry("杂种", "abuse", RISK_MEDIUM),
            new WordEntry("草泥马", "abuse", RISK_MEDIUM),
            new WordEntry("娘炮", "abuse", RISK_MEDIUM),
            new WordEntry("死胖子", "abuse", RISK_MEDIUM),
            new WordEntry("肥猪", "abuse", RISK_MEDIUM),
            new WordEntry("土鳖", "abuse", RISK_MEDIUM),
            new WordEntry("穷鬼", "abuse", RISK_MEDIUM),
            new WordEntry("乡巴佬", "abuse", RISK_MEDIUM),
            new WordEntry("滚蛋", "abuse", RISK_MEDIUM),
            new WordEntry("去死吧", "abuse", RISK_MEDIUM),
            new WordEntry("死全家", "abuse", RISK_MEDIUM),
            new WordEntry("断子绝孙", "abuse", RISK_MEDIUM),
            new WordEntry("不得好死", "abuse", RISK_MEDIUM),
            new WordEntry("贱骨头", "abuse", RISK_MEDIUM),
            new WordEntry("贱女人", "abuse", RISK_MEDIUM),
            new WordEntry("臭婊子", "abuse", RISK_MEDIUM),
            new WordEntry("婊子养的", "abuse", RISK_MEDIUM),
            new WordEntry("二奶", "abuse", RISK_MEDIUM),
            new WordEntry("野种", "abuse", RISK_MEDIUM),
            new WordEntry("小杂种", "abuse", RISK_MEDIUM),
            new WordEntry("兔崽子", "abuse", RISK_MEDIUM),
            new WordEntry("龟儿子", "abuse", RISK_MEDIUM),
            new WordEntry("王八羔子", "abuse", RISK_MEDIUM),
            new WordEntry("狗东西", "abuse", RISK_MEDIUM),
            new WordEntry("狗娘养的", "abuse", RISK_MEDIUM),
            new WordEntry("狼心狗肺", "abuse", RISK_MEDIUM),
            new WordEntry("猪狗不如", "abuse", RISK_MEDIUM),
            new WordEntry("畜生不如", "abuse", RISK_MEDIUM),
            new WordEntry("人渣", "abuse", RISK_MEDIUM),
            new WordEntry("败类", "abuse", RISK_MEDIUM),
            new WordEntry("社会渣滓", "abuse", RISK_MEDIUM),
            new WordEntry("下等人", "abuse", RISK_MEDIUM),
            new WordEntry("窝囊废", "abuse", RISK_MEDIUM),
            new WordEntry("软骨头", "abuse", RISK_MEDIUM),
            new WordEntry("舔狗", "abuse", RISK_MEDIUM),
            new WordEntry("狗腿子", "abuse", RISK_MEDIUM),
            new WordEntry("汉奸", "abuse", RISK_MEDIUM),
            new WordEntry("卖国贼", "abuse", RISK_MEDIUM),
            new WordEntry("走狗", "abuse", RISK_MEDIUM),
            new WordEntry("菜鸡", "abuse", RISK_MEDIUM),
            new WordEntry("辣鸡", "abuse", RISK_MEDIUM),
            new WordEntry("菜逼", "abuse", RISK_MEDIUM),
            new WordEntry("蠢猪", "abuse", RISK_MEDIUM),
            new WordEntry("笨猪", "abuse", RISK_MEDIUM),
            new WordEntry("蠢驴", "abuse", RISK_MEDIUM),
            new WordEntry("脑子进水", "abuse", RISK_MEDIUM),
            new WordEntry("脑子有病", "abuse", RISK_MEDIUM),
            new WordEntry("缺根筋", "abuse", RISK_MEDIUM),
            new WordEntry("没脑子", "abuse", RISK_MEDIUM),
            new WordEntry("不要脸", "abuse", RISK_MEDIUM),
            new WordEntry("臭不要脸", "abuse", RISK_MEDIUM),
            new WordEntry("厚颜无耻", "abuse", RISK_MEDIUM),
            new WordEntry("人妖", "abuse", RISK_MEDIUM),
            new WordEntry("死人妖", "abuse", RISK_MEDIUM),
            new WordEntry("不男不女", "abuse", RISK_MEDIUM),

            // ===== 广告营销 / 诈骗引流类（MEDIUM/LOW）=====
            new WordEntry("加微信", "ads", RISK_MEDIUM),
            new WordEntry("代开发票", "ads", RISK_MEDIUM),
            new WordEntry("刷单兼职", "ads", RISK_MEDIUM),
            new WordEntry("微信转账", "ads", RISK_MEDIUM),
            new WordEntry("微信代购", "ads", RISK_MEDIUM),
            new WordEntry("微信代理", "ads", RISK_MEDIUM),
            new WordEntry("微信分销", "ads", RISK_MEDIUM),
            new WordEntry("淘宝刷单", "ads", RISK_MEDIUM),
            new WordEntry("京东刷单", "ads", RISK_MEDIUM),
            new WordEntry("拼多多刷单", "ads", RISK_MEDIUM),
            new WordEntry("刷信誉", "ads", RISK_MEDIUM),
            new WordEntry("刷好评", "ads", RISK_MEDIUM),
            new WordEntry("打字员兼职", "ads", RISK_MEDIUM),
            new WordEntry("点赞赚钱", "ads", RISK_MEDIUM),
            new WordEntry("关注赚钱", "ads", RISK_MEDIUM),
            new WordEntry("转发赚钱", "ads", RISK_MEDIUM),
            new WordEntry("挂机赚钱", "ads", RISK_MEDIUM),
            new WordEntry("日赚千元", "ads", RISK_MEDIUM),
            new WordEntry("月入十万", "ads", RISK_MEDIUM),
            new WordEntry("躺赚项目", "ads", RISK_MEDIUM),
            new WordEntry("被动收入", "ads", RISK_MEDIUM),
            new WordEntry("暴富项目", "ads", RISK_MEDIUM),
            new WordEntry("一夜暴富", "ads", RISK_MEDIUM),
            new WordEntry("空手套白狼", "ads", RISK_MEDIUM),
            new WordEntry("0元购", "ads", RISK_LOW),
            new WordEntry("限时秒杀", "ads", RISK_LOW),
            new WordEntry("清仓甩卖", "ads", RISK_LOW),
            new WordEntry("扫码领红包", "ads", RISK_LOW),
            new WordEntry("集赞免费送", "ads", RISK_LOW),
            new WordEntry("转发有礼", "ads", RISK_LOW),
            new WordEntry("分享返利", "ads", RISK_LOW),
            new WordEntry("内部优惠券", "ads", RISK_LOW),
            new WordEntry("高仿包包", "ads", RISK_MEDIUM),
            new WordEntry("高仿手表", "ads", RISK_MEDIUM),
            new WordEntry("高仿鞋", "ads", RISK_MEDIUM),
            new WordEntry("精仿手机", "ads", RISK_MEDIUM),
            new WordEntry("A货批发", "ads", RISK_MEDIUM),
            new WordEntry("一比一复刻", "ads", RISK_MEDIUM),
            new WordEntry("原单尾货", "ads", RISK_LOW),
            new WordEntry("拉人头赚钱", "ads", RISK_MEDIUM),
            new WordEntry("层级分销", "ads", RISK_MEDIUM),
            new WordEntry("入门费代理", "ads", RISK_MEDIUM),
            new WordEntry("外汇喊单", "ads", RISK_MEDIUM),
            new WordEntry("期货喊单", "ads", RISK_MEDIUM),
            new WordEntry("股票带单", "ads", RISK_MEDIUM),
            new WordEntry("荐股老师", "ads", RISK_MEDIUM),
            new WordEntry("直播带单", "ads", RISK_MEDIUM),
            new WordEntry("viagra", "ads", RISK_LOW),
            new WordEntry("微商推广", "ads", RISK_LOW),
            new WordEntry("群发广告", "ads", RISK_LOW),
            new WordEntry("促销广告", "ads", RISK_LOW),
            new WordEntry("免费领取", "ads", RISK_LOW),

            // ===== 补充：辱骂类（MEDIUM）=====
            new WordEntry("尼玛", "abuse", RISK_MEDIUM),
            new WordEntry("特么", "abuse", RISK_MEDIUM),
            new WordEntry("妈蛋", "abuse", RISK_MEDIUM),
            new WordEntry("卧槽", "abuse", RISK_MEDIUM),
            new WordEntry("我操", "abuse", RISK_MEDIUM),
            new WordEntry("滚犊子", "abuse", RISK_MEDIUM),
            new WordEntry("扯犊子", "abuse", RISK_MEDIUM),
            new WordEntry("瘪三", "abuse", RISK_MEDIUM),
            new WordEntry("贱婢", "abuse", RISK_MEDIUM),
            new WordEntry("淫妇", "abuse", RISK_MEDIUM),
            new WordEntry("荡妇", "abuse", RISK_MEDIUM),
            new WordEntry("破鞋", "abuse", RISK_MEDIUM),
            new WordEntry("老不死", "abuse", RISK_MEDIUM),
            new WordEntry("怂货", "abuse", RISK_MEDIUM),
            new WordEntry("软蛋", "abuse", RISK_MEDIUM),
            new WordEntry("没种", "abuse", RISK_MEDIUM),
            new WordEntry("孬种", "abuse", RISK_MEDIUM),
            new WordEntry("胆小鬼", "abuse", RISK_MEDIUM),
            new WordEntry("废物点心", "abuse", RISK_MEDIUM),
            new WordEntry("脑子瓦特", "abuse", RISK_MEDIUM),

            // ===== 补充：广告营销类（MEDIUM/LOW）=====
            new WordEntry("加V信", "ads", RISK_MEDIUM),
            new WordEntry("扫码领福利", "ads", RISK_LOW),
            new WordEntry("进群领资料", "ads", RISK_LOW),
            new WordEntry("点击下方链接", "ads", RISK_LOW),
            new WordEntry("长按识别二维码", "ads", RISK_LOW),
            new WordEntry("关注公众号领", "ads", RISK_LOW),
            new WordEntry("转发此条", "ads", RISK_LOW),
            new WordEntry("抽奖必中", "ads", RISK_LOW),
            new WordEntry("低价代购", "ads", RISK_LOW),
            new WordEntry("厂家直销", "ads", RISK_LOW),
            new WordEntry("一手货源", "ads", RISK_LOW),
            new WordEntry("微商货源", "ads", RISK_LOW),
            new WordEntry("加盟代理", "ads", RISK_MEDIUM),
            new WordEntry("招商加盟", "ads", RISK_MEDIUM),
            new WordEntry("稳赚不赔项目", "ads", RISK_MEDIUM),
            new WordEntry("免费送宠物", "ads", RISK_LOW),

            // ===== 补充：诈骗类（HIGH）=====
            new WordEntry("冒充班主任收费", "scam", RISK_HIGH),
            new WordEntry("培训费诈骗", "scam", RISK_HIGH),
            new WordEntry("补课费诈骗", "scam", RISK_HIGH),
            new WordEntry("助学金诈骗", "scam", RISK_HIGH),
            new WordEntry("贫困补助诈骗", "scam", RISK_HIGH),
            new WordEntry("游戏解封诈骗", "scam", RISK_HIGH),
            new WordEntry("账号找回诈骗", "scam", RISK_HIGH),
            new WordEntry("学历提升诈骗", "scam", RISK_HIGH),
            new WordEntry("机票退改签诈骗", "scam", RISK_HIGH),
            new WordEntry("快递丢失理赔", "scam", RISK_HIGH),
            new WordEntry("冒充物流理赔", "scam", RISK_HIGH),
            new WordEntry("社保卡升级诈骗", "scam", RISK_HIGH),
            new WordEntry("医保骗保", "scam", RISK_HIGH),
            new WordEntry("购物卡回收", "scam", RISK_MEDIUM),
            new WordEntry("加油卡回收", "scam", RISK_MEDIUM),
            new WordEntry("美女诱导投资", "scam", RISK_HIGH),
            new WordEntry("网恋投资诈骗", "scam", RISK_HIGH),
            new WordEntry("博彩套利", "scam", RISK_HIGH),

            // ===== 补充：违法类（HIGH）=====
            new WordEntry("走私手机", "illegal", RISK_HIGH),
            new WordEntry("走私化妆品", "illegal", RISK_HIGH),
            new WordEntry("野味销售", "illegal", RISK_HIGH),
            new WordEntry("捕鸟网销售", "illegal", RISK_HIGH),
            new WordEntry("瞄准镜销售", "illegal", RISK_HIGH),
            new WordEntry("蝴蝶刀", "illegal", RISK_HIGH),
            new WordEntry("甩棍销售", "illegal", RISK_HIGH),
            new WordEntry("弓箭销售", "illegal", RISK_HIGH),
            new WordEntry("翻墙账号", "illegal", RISK_HIGH),
            new WordEntry("钓鱼网站", "illegal", RISK_HIGH),
            new WordEntry("数据泄露贩卖", "illegal", RISK_HIGH),
            new WordEntry("企业号买卖", "illegal", RISK_HIGH),
            new WordEntry("代办公司注册", "illegal", RISK_MEDIUM),
            new WordEntry("假公文印章", "illegal", RISK_HIGH),

            // ===== 补充：赌博类（HIGH）=====
            new WordEntry("百家乐", "gambling", RISK_HIGH),
            new WordEntry("龙虎斗", "gambling", RISK_HIGH),
            new WordEntry("押大小", "gambling", RISK_HIGH),
            new WordEntry("麻将赌钱", "gambling", RISK_HIGH),
            new WordEntry("牌九赌博", "gambling", RISK_HIGH),
            new WordEntry("六合彩", "gambling", RISK_HIGH),
            new WordEntry("红包群赌博", "gambling", RISK_HIGH),
            new WordEntry("麻将馆赌钱", "gambling", RISK_HIGH),
            new WordEntry("棋牌室赌钱", "gambling", RISK_HIGH),
            new WordEntry("赌局抽水", "gambling", RISK_HIGH),

            // ===== 补充：毒品类（HIGH）=====
            new WordEntry("麻古", "drugs", RISK_HIGH),
            new WordEntry("开心粉", "drugs", RISK_HIGH),
            new WordEntry("奶茶粉", "drugs", RISK_HIGH),
            new WordEntry("飞叶子", "drugs", RISK_HIGH),
            new WordEntry("嗑药", "drugs", RISK_HIGH),
            new WordEntry("嗨药", "drugs", RISK_HIGH),
            new WordEntry("大麻烟", "drugs", RISK_HIGH),
            new WordEntry("紫水", "drugs", RISK_HIGH),

            // ===== 补充：色情类（HIGH）=====
            new WordEntry("黄色笑话", "porn", RISK_MEDIUM),
            new WordEntry("成人写真", "porn", RISK_HIGH),
            new WordEntry("性玩具", "porn", RISK_HIGH),
            new WordEntry("情趣用品", "porn", RISK_MEDIUM),
            new WordEntry("陪聊服务", "porn", RISK_MEDIUM)
    );

    /**
     * 未成年加强词表：仅未成年保护模式生效时额外命中。
     * 这些词本身不进入主词库（普通用户正常放行），但对未成年用户更严格。
     * 必须保证这些词不是主词库词条的子串，否则普通用户也会命中，破坏"更严格"语义。
     */
    public static final List<WordEntry> MINOR_EXTRA = List.of(
            new WordEntry("福利姬", "minor", RISK_MEDIUM),
            new WordEntry("福利视频", "minor", RISK_MEDIUM),
            new WordEntry("福利图包", "minor", RISK_MEDIUM),
            new WordEntry("无码写真", "minor", RISK_MEDIUM),
            new WordEntry("无码视频", "minor", RISK_MEDIUM),
            new WordEntry("深夜陪聊", "minor", RISK_MEDIUM),
            new WordEntry("上门按摩", "minor", RISK_MEDIUM),
            new WordEntry("豪车接送", "minor", RISK_MEDIUM),
            new WordEntry("空姐伴游", "minor", RISK_MEDIUM),
            new WordEntry("嫩模伴游", "minor", RISK_MEDIUM),
            new WordEntry("资源分享群", "minor", RISK_MEDIUM),
            new WordEntry("付费写真", "minor", RISK_MEDIUM),
            new WordEntry("私拍写真", "minor", RISK_MEDIUM),
            new WordEntry("大尺度写真", "minor", RISK_MEDIUM),
            new WordEntry("擦边球直播", "minor", RISK_MEDIUM),
            new WordEntry("低俗直播", "minor", RISK_MEDIUM),
            new WordEntry("软色情", "minor", RISK_MEDIUM),
            new WordEntry("性暗示", "minor", RISK_MEDIUM),
            new WordEntry("开房约见", "minor", RISK_MEDIUM),
            new WordEntry("同城开房", "minor", RISK_MEDIUM),
            new WordEntry("校园裸贷", "minor", RISK_MEDIUM),
            new WordEntry("美女主播私聊", "minor", RISK_MEDIUM)
    );

    /** 全词条（含未成年加强）小写索引，成员判断 O(1)。 */
    private static final Set<String> WORD_INDEX = new HashSet<>();

    static {
        for (WordEntry e : BUILT_IN) {
            WORD_INDEX.add(e.word().toLowerCase());
        }
        for (WordEntry e : MINOR_EXTRA) {
            WORD_INDEX.add(e.word().toLowerCase());
        }
    }

    /** 外部动态加载的词（预留数据库扩展点）。 */
    private final Map<String, WordEntry> external = new ConcurrentHashMap<>();

    /** 全部词条 = 内置 + 外部动态注册。 */
    public List<WordEntry> allEntries() {
        List<WordEntry> all = new ArrayList<>(BUILT_IN);
        all.addAll(external.values());
        return all;
    }

    /** 未成年加强词表（仅未成年模式生效时由审核服务额外扫描）。 */
    public List<WordEntry> minorExtraEntries() {
        return MINOR_EXTRA;
    }

    /** 词库总条数（内置，不含动态注册），供审计报告/测试断言使用。 */
    public int builtInSize() {
        return BUILT_IN.size();
    }

    /** 是否为内置敏感词（大小写不敏感，O(1) HashSet 查找）。 */
    public boolean containsWord(String word) {
        if (word == null || word.isBlank()) {
            return false;
        }
        return WORD_INDEX.contains(word.trim().toLowerCase());
    }

    /** 预留扩展点：从数据库加载词库后调用，注册额外敏感词。 */
    public void register(String word, String category, String riskLevel) {
        if (word == null || word.isBlank()) {
            return;
        }
        external.put(word.toLowerCase(), new WordEntry(word, category, riskLevel));
    }
}
