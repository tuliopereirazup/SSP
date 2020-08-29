package org.jasig.ssp.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.util.DateTimeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

class GetMinimumResponseComplianceDateUseCase {

    private final ConfigService configService;

    @Autowired
    public GetMinimumResponseComplianceDateUseCase(ConfigService configService) {
        this.configService = configService;
    }

    public Date execute() {
        final String numVal = configService
                .getByNameNull("maximum_days_before_early_alert_response");
        if(StringUtils.isBlank(numVal))
            return null;
        int allowedDaysPastResponse = Integer.parseInt(numVal);

        return DateTimeUtils.getDateOffsetInDays(new Date(), -allowedDaysPastResponse);
    }
}
