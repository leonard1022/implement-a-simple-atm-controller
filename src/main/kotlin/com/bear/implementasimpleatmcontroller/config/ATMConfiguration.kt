package com.bear.implementasimpleatmcontroller.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "atm.limits")
class ATMConfiguration {

    var transaction = TransactionLimits()
    var daily = DailyLimits()

    class TransactionLimits {
        var minAmount: Int = 1
        var maxSingleDeposit: Int = 10000
        var maxSingleWithdrawal: Int = 5000
    }

    class DailyLimits {
        var maxDeposit: Int = 50000
        var maxWithdrawal: Int = 10000
    }
}