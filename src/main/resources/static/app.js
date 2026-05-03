const form = document.getElementById("contractForm");
const formMessage = document.getElementById("formMessage");
const formTitle = document.getElementById("formTitle");
const submitButton = document.getElementById("submitButton");
const cancelEditButton = document.getElementById("cancelEditButton");
const contractList = document.getElementById("contractList");
const profitList = document.getElementById("profitList");
const reminderList = document.getElementById("reminderList");
const statsList = document.getElementById("statsList");
const reminderFilter = document.getElementById("reminderFilter");
const dateInputs = document.querySelectorAll(".date-input");

const reminderTemplate = document.getElementById("reminderCardTemplate");
const contractTemplate = document.getElementById("contractCardTemplate");
const profitTemplate = document.getElementById("profitCardTemplate");
const statsTemplate = document.getElementById("statsCardTemplate");

const moneyFormatter = new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2
});

const dateFormatter = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
});

let dashboardState = {
    contracts: [],
    reminders: [],
    contractStats: [],
    profitStats: [],
    inputSuggestions: {}
};

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json"
        },
        ...options
    });

    if (!response.ok) {
        let message = "请求失败";
        try {
            const data = await response.json();
            message = data.message || message;
        } catch (error) {
            message = response.statusText || message;
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("Content-Type") || "";
    return contentType.includes("application/json") ? response.json() : null;
}

async function loadDashboard() {
    dashboardState = await request("/api/dashboard");
    renderSuggestions(dashboardState.inputSuggestions);
    renderContracts(dashboardState.contracts);
    renderProfitStats(dashboardState.profitStats);
    renderReminders(dashboardState.reminders);
    renderStats(dashboardState.contractStats);
}

function renderSuggestions(inputSuggestions) {
    updateDatalist("signatoryCompanyList", inputSuggestions.signatoryCompanies || []);
    updateDatalist("counterpartyCompanyList", inputSuggestions.counterpartyCompanies || []);
    updateDatalist("projectNameList", inputSuggestions.projectNames || []);
    updateDatalist("signedAreaList", inputSuggestions.signedAreas || []);
}

function updateDatalist(elementId, values) {
    const datalist = document.getElementById(elementId);
    datalist.innerHTML = "";
    values.forEach((value) => {
        const option = document.createElement("option");
        option.value = value;
        datalist.appendChild(option);
    });
}

function renderContracts(contracts) {
    contractList.innerHTML = "";

    if (!contracts.length) {
        contractList.className = "contract-list empty-state";
        contractList.textContent = "暂无合同，请先录入。";
        return;
    }

    contractList.className = "contract-list";
    contracts.forEach((contract) => {
        const fragment = contractTemplate.content.cloneNode(true);
        const card = fragment.querySelector(".contract-card");
        applyTypeClass(card, contract.type);
        fragment.querySelector(".type-pill").textContent = contract.typeLabel;
        fragment.querySelector(".project-name").textContent = contract.projectName;
        fragment.querySelector(".company-name").textContent = `${contract.signatoryCompany} / ${contract.counterpartyCompany}`;
        fragment.querySelector(".total-amount").textContent = moneyFormatter.format(contract.totalAmount);
        fragment.querySelector(".total-cycles").textContent = `${contract.totalCycles} 个`;
        fragment.querySelector(".chargeable-months").textContent = `${contract.chargeableMonths} 个月`;

        const detailGrid = fragment.querySelector(".detail-grid");
        buildDetailItems([
            ["签约时间", formatDate(contract.signingDate)],
            ["签约面积", contract.signedArea],
            ["起租时间", formatDate(contract.leaseStartDate)],
            ["免租截止日", contract.rentFreeEndDate ? formatDate(contract.rentFreeEndDate) : "无"],
            ["合同终止时间", formatDate(contract.contractEndDate)],
            ["月租金", moneyFormatter.format(contract.monthlyRent)],
            ["月物业费", moneyFormatter.format(contract.monthlyPropertyFee)],
            ["缴费周期", `${contract.paymentCycleMonths} 个月`],
            ["每周期金额", moneyFormatter.format(contract.cycleAmount)],
            ["累计实收实付", moneyFormatter.format(contract.cumulativeActualAmount)],
            ["已完成周期", `${contract.settledCycles} 个`]
        ]).forEach((item) => detailGrid.appendChild(item));

        fragment.querySelector(".edit-btn").addEventListener("click", () => startEdit(contract));
        fragment.querySelector(".delete-btn").addEventListener("click", () => removeContract(contract.id));

        contractList.appendChild(fragment);
    });
}

function renderReminders(reminders) {
    reminderList.innerHTML = "";
    const filterValue = reminderFilter.value;
    const visibleReminders = reminders.filter((item) => filterValue === "ALL" || item.type === filterValue);

    if (!visibleReminders.length) {
        reminderList.className = "card-stack empty-state";
        reminderList.textContent = "暂无提醒";
        return;
    }

    reminderList.className = "card-stack";
    buildGroupedSections(visibleReminders, reminderList, renderReminderCard, {
        RECEIVABLE: "收款提醒",
        PAYABLE: "付款提醒"
    }, "reminder-group");
}

function renderReminderCard(reminder, container) {
    const fragment = reminderTemplate.content.cloneNode(true);
    const card = fragment.querySelector(".reminder-card");
    applyTypeClass(card, reminder.type);
    fragment.querySelector(".type-pill").textContent = reminder.typeLabel;
    fragment.querySelector(".project-name").textContent = reminder.projectName;
    fragment.querySelector(".company-name").textContent = reminder.signatoryCompany;
    fragment.querySelector(".counterparty-name").textContent = reminder.counterpartyCompany;
    fragment.querySelector(".cycle-amount").textContent = moneyFormatter.format(reminder.cycleAmount);
    fragment.querySelector(".cycle-range").textContent =
        `${formatDate(reminder.cycleStartDate)} - ${formatDate(reminder.cycleEndDate)}`;
    fragment.querySelector(".due-date").textContent = formatDate(reminder.dueDate);
    fragment.querySelector(".status-text").textContent = reminder.daysUntilDue >= 0
        ? `距离缴费日 ${reminder.daysUntilDue} 天`
        : `已逾期 ${Math.abs(reminder.daysUntilDue)} 天`;
    fragment.querySelector(".progress-bar").style.width = `${Math.max(0, reminder.progressPercent)}%`;

    fragment.querySelector(".complete-btn").addEventListener("click", () => {
        settleCycle(reminder.contractId, reminder.cycleKey, { fullPayment: true });
    });

    container.appendChild(fragment);
}

function renderStats(contractStats) {
    statsList.innerHTML = "";

    if (!contractStats.length) {
        statsList.className = "card-stack empty-state";
        statsList.textContent = "暂无合同统计";
        return;
    }

    statsList.className = "card-stack";
    buildGroupedSections(contractStats, statsList, renderStatsCard, {
        RECEIVABLE: "收款合同",
        PAYABLE: "付款合同"
    }, "stats-group");
}

function renderProfitStats(profitStats) {
    profitList.innerHTML = "";

    if (!profitStats.length) {
        profitList.className = "profit-list empty-state";
        profitList.textContent = "暂无利润统计";
        return;
    }

    profitList.className = "profit-list";
    profitStats.forEach((stat) => {
        const fragment = profitTemplate.content.cloneNode(true);
        const gapNode = fragment.querySelector(".gap-amount");
        fragment.querySelector(".project-name").textContent = stat.projectName;
        fragment.querySelector(".company-name").textContent = stat.signatoryCompany;
        fragment.querySelector(".cumulative-due-amount").textContent = moneyFormatter.format(stat.cumulativeDueAmount);
        fragment.querySelector(".cumulative-actual-amount").textContent = moneyFormatter.format(stat.cumulativeActualAmount);
        gapNode.textContent = moneyFormatter.format(stat.gapAmount);
        gapNode.classList.toggle("gap-negative", Number(stat.gapAmount) < 0);
        gapNode.classList.toggle("gap-positive", Number(stat.gapAmount) >= 0);
        profitList.appendChild(fragment);
    });
}

function renderStatsCard(stat, container) {
    const fragment = statsTemplate.content.cloneNode(true);
    const card = fragment.querySelector(".stats-card");
    const labels = getAmountLabels(stat.type);
    applyTypeClass(card, stat.type);

    fragment.querySelector(".type-pill").textContent = stat.typeLabel;
    fragment.querySelector(".project-name").textContent = stat.projectName;
    fragment.querySelector(".company-name").textContent = `${stat.signatoryCompany} / ${stat.counterpartyCompany}`;
    fragment.querySelector(".current-label").textContent = labels.current;
    fragment.querySelector(".cumulative-due-label").textContent = labels.cumulativeDue;
    fragment.querySelector(".cumulative-actual-label").textContent = labels.cumulativeActual;
    fragment.querySelector(".current-cycle-text").textContent = stat.cycleStartDate
        ? `当前周期：${formatDate(stat.cycleStartDate)} - ${formatDate(stat.cycleEndDate)}`
        : "";
    fragment.querySelector(".current-amount").textContent = moneyFormatter.format(stat.currentCycleAmount);
    fragment.querySelector(".cumulative-due-amount").textContent = moneyFormatter.format(stat.cumulativeDueAmount);
    fragment.querySelector(".cumulative-actual-amount").textContent = moneyFormatter.format(stat.cumulativeActualAmount);
    const overdueNode = fragment.querySelector(".overdue-amount");
    overdueNode.textContent = moneyFormatter.format(stat.overdueAmount);
    overdueNode.classList.toggle("overdue-positive", Number(stat.overdueAmount) > 0);
    fragment.querySelector(".status-text").textContent = stat.settled
        ? "全部完成"
        : (stat.daysUntilDue >= 0 ? `距离缴费日 ${stat.daysUntilDue} 天` : `已逾期 ${Math.abs(stat.daysUntilDue)} 天`);
    fragment.querySelector(".progress-bar").style.width = `${Math.max(0, stat.progressPercent)}%`;

    const settledView = fragment.querySelector(".settled-view");
    const activeSettleView = fragment.querySelector(".active-settle-view");
    if (stat.settled) {
        settledView.hidden = false;
        activeSettleView.hidden = true;
    } else {
        settledView.hidden = true;
        activeSettleView.hidden = false;
        const actualAmountInput = fragment.querySelector(".actual-amount-input");

        fragment.querySelector(".full-btn").textContent = stat.type === "RECEIVABLE" ? "全额收款" : "全额付款";
        fragment.querySelector(".manual-btn").textContent = "记录并进入下一期";
        fragment.querySelector(".full-btn").addEventListener("click", () => {
            settleCycle(stat.contractId, stat.currentCycleKey, { fullPayment: true });
        });
        fragment.querySelector(".manual-btn").addEventListener("click", () => {
            const actualAmount = actualAmountInput.value.trim();
            if (!actualAmount) {
                window.alert("请输入本期实收实付款");
                return;
            }
            settleCycle(stat.contractId, stat.currentCycleKey, {
                fullPayment: false,
                actualAmount: Number(actualAmount)
            });
        });
    }

    container.appendChild(fragment);
}

function buildGroupedSections(items, target, renderer, titleMap, groupClassName = "stats-group") {
    const groups = [
        { key: "RECEIVABLE", title: titleMap.RECEIVABLE },
        { key: "PAYABLE", title: titleMap.PAYABLE }
    ];

    groups.forEach((group) => {
        const groupItems = items.filter((item) => item.type === group.key);
        if (!groupItems.length) {
            return;
        }

        const wrapper = document.createElement("section");
        wrapper.className = groupClassName;
        const title = document.createElement("h3");
        title.className = "group-title";
        title.textContent = group.title;
        wrapper.appendChild(title);
        groupItems.forEach((item) => renderer(item, wrapper));
        target.appendChild(wrapper);
    });
}

function buildDetailItems(entries) {
    return entries.map(([label, value]) => {
        const wrapper = document.createElement("div");
        const labelNode = document.createElement("span");
        const valueNode = document.createElement("strong");
        labelNode.textContent = label;
        valueNode.textContent = value;
        wrapper.append(labelNode, valueNode);
        return wrapper;
    });
}

function getAmountLabels(type) {
    return type === "RECEIVABLE"
        ? { current: "本期应收款", cumulativeDue: "累计应收款", cumulativeActual: "累计实收款" }
        : { current: "本期应付款", cumulativeDue: "累计应付款", cumulativeActual: "累计实付款" };
}

function applyTypeClass(element, type) {
    element.classList.remove("type-receivable", "type-payable");
    element.classList.add(type === "RECEIVABLE" ? "type-receivable" : "type-payable");
}

function formatDate(value) {
    return dateFormatter.format(new Date(value));
}

function startEdit(contract) {
    form.contractId.value = contract.id;
    form.type.value = contract.type;
    form.signatoryCompany.value = contract.signatoryCompany;
    form.counterpartyCompany.value = contract.counterpartyCompany;
    form.projectName.value = contract.projectName;
    form.signingDate.value = contract.signingDate;
    form.signedArea.value = contract.signedArea;
    form.leaseStartDate.value = contract.leaseStartDate;
    form.rentFreeEndDate.value = contract.rentFreeEndDate || "";
    form.contractEndDate.value = contract.contractEndDate;
    form.monthlyRent.value = contract.monthlyRent;
    form.monthlyPropertyFee.value = contract.monthlyPropertyFee;
    form.paymentCycleMonths.value = contract.paymentCycleMonths;

    formTitle.textContent = "修改合同";
    submitButton.textContent = "保存修改";
    cancelEditButton.hidden = false;
    refreshDateInputState();
    form.scrollIntoView({ behavior: "smooth", block: "start" });
}

function resetForm() {
    form.reset();
    form.contractId.value = "";
    formTitle.textContent = "录入合同";
    submitButton.textContent = "保存合同";
    cancelEditButton.hidden = true;
    refreshDateInputState();
}

async function removeContract(contractId) {
    if (!window.confirm("确认删除这份合同吗？")) {
        return;
    }
    try {
        await request(`/api/contracts/${contractId}`, { method: "DELETE" });
        if (form.contractId.value === contractId) {
            resetForm();
        }
        formMessage.textContent = "合同已删除";
        await loadDashboard();
    } catch (error) {
        window.alert(error.message);
    }
}

async function settleCycle(contractId, cycleKey, payload) {
    try {
        await request(`/api/contracts/${contractId}/cycles/${cycleKey}/settle`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        await loadDashboard();
    } catch (error) {
        window.alert(error.message);
    }
}

cancelEditButton.addEventListener("click", () => {
    resetForm();
    formMessage.textContent = "";
});

reminderFilter.addEventListener("change", () => {
    renderReminders(dashboardState.reminders);
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    formMessage.textContent = "正在保存...";

    const formData = new FormData(form);
    const payload = Object.fromEntries(formData.entries());
    const contractId = payload.contractId;
    delete payload.contractId;

    if (!payload.rentFreeEndDate) {
        payload.rentFreeEndDate = null;
    }

    payload.monthlyRent = Number(payload.monthlyRent);
    payload.monthlyPropertyFee = Number(payload.monthlyPropertyFee);
    payload.paymentCycleMonths = Number(payload.paymentCycleMonths);

    try {
        const method = contractId ? "PUT" : "POST";
        const url = contractId ? `/api/contracts/${contractId}` : "/api/contracts";
        await request(url, {
            method,
            body: JSON.stringify(payload)
        });
        resetForm();
        formMessage.textContent = contractId ? "保存成功" : "合同保存成功";
        await loadDashboard();
    } catch (error) {
        formMessage.textContent = error.message;
    }
});

function refreshDateInputState() {
    dateInputs.forEach((input) => {
        input.classList.toggle("has-value", Boolean(input.value));
    });
}

dateInputs.forEach((input) => {
    input.addEventListener("change", refreshDateInputState);
    input.addEventListener("input", refreshDateInputState);
});

loadDashboard().catch((error) => {
    formMessage.textContent = error.message;
});

refreshDateInputState();
