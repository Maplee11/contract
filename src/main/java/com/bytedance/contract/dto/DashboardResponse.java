package com.bytedance.contract.dto;

import java.util.List;

public record DashboardResponse(
        List<ContractView> contracts,
        List<ReminderView> reminders,
        List<ContractStatsView> contractStats,
        List<ProfitStatView> profitStats,
        InputSuggestions inputSuggestions
) {
}
