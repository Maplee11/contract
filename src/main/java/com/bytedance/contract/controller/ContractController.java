package com.bytedance.contract.controller;

import com.bytedance.contract.dto.ContractRequest;
import com.bytedance.contract.dto.DashboardResponse;
import com.bytedance.contract.dto.SettleCycleRequest;
import com.bytedance.contract.service.ContractService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return contractService.getDashboard();
    }

    @PostMapping("/contracts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createContract(@RequestBody ContractRequest request) {
        return Map.of("id", contractService.createContract(request).getId().toString());
    }

    @PutMapping("/contracts/{contractId}")
    public Map<String, String> updateContract(@PathVariable UUID contractId, @RequestBody ContractRequest request) {
        return Map.of("id", contractService.updateContract(contractId, request).getId().toString());
    }

    @DeleteMapping("/contracts/{contractId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContract(@PathVariable UUID contractId) {
        contractService.deleteContract(contractId);
    }

    @PostMapping("/contracts/{contractId}/cycles/{cycleKey}/settle")
    public void settleCycle(
            @PathVariable UUID contractId,
            @PathVariable String cycleKey,
            @RequestBody(required = false) SettleCycleRequest request
    ) {
        contractService.settleCycle(contractId, cycleKey, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
