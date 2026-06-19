package com.citygo.tourism.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body) {
        return Map.of(
                "realData", false,
                "message", "AI 助手接口已预留。请接入合规大模型服务后启用；当前不会基于非真实数据生成价格或行程结论。",
                "input", body);
    }
}
