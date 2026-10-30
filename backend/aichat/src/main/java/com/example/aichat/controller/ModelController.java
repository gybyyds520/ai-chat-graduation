package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.AiModel;
import com.example.aichat.service.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping("/list")
    public R getAvailableModels() {
        List<AiModel> models = modelService.getAvailableModels();
        return R.success(models);
    }

    @GetMapping("/category/{category}")
    public R getModelsByCategory(@PathVariable String category) {
        List<AiModel> models = modelService.getModelsByCategory(category);
        return R.success(models);
    }

    @GetMapping("/{modelKey}")
    public R getModelByKey(@PathVariable String modelKey) {
        AiModel model = modelService.getModelByKey(modelKey);
        if (model != null) {
            return R.success(model);
        }
        return R.error("模型不存在");
    }

    @PostMapping("/add")
    public R addModel(@RequestBody AiModel model) {
        modelService.addModel(model);
        return R.success("模型添加成功");
    }

    @PutMapping("/{id}")
    public R updateModel(@PathVariable Long id, @RequestBody AiModel model) {
        modelService.updateModel(id, model);
        return R.success("模型更新成功");
    }

    @DeleteMapping("/{id}")
    public R deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return R.success("模型删除成功");
    }

    @PostMapping("/{id}/toggle")
    public R toggleModelStatus(@PathVariable Long id, @RequestBody Boolean available) {
        modelService.toggleModelStatus(id, available);
        return R.success("状态已更新");
    }
}