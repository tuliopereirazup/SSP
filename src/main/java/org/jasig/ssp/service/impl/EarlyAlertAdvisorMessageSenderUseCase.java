package org.jasig.ssp.service.impl;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.model.*;
import org.jasig.ssp.service.EarlyAlertRoutingService;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.SendFailedException;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class EarlyAlertAdvisorMessageSenderUseCase {

    private final MessageTemplateService messageTemplateService;
    private final EarlyAlertRoutingService earlyAlertRoutingService;
    private final MessageService messageService;
    private final FillTemplateParametersUseCase fillTemplateParametersUseCase;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertServiceImpl.class);

    @Autowired
    public EarlyAlertAdvisorMessageSenderUseCase(
            MessageTemplateService messageTemplateService,
            EarlyAlertRoutingService earlyAlertRoutingService,
            MessageService messageService,
            FillTemplateParametersUseCase fillTemplateParametersUseCase) {
        this.messageTemplateService = messageTemplateService;
        this.earlyAlertRoutingService = earlyAlertRoutingService;
        this.messageService = messageService;
        this.fillTemplateParametersUseCase = fillTemplateParametersUseCase;
    }

    /**
     * Send e-mail ({@link Message}) to the assigned advisor for the student.
     *
     * @param earlyAlert
     *            Early Alert
     * @param emailCC
     *            Email address to also CC this message
     * @throws ObjectNotFoundException
     * @throws ValidationException
     */
    public void execute(@NotNull final EarlyAlert earlyAlert, // NOPMD
                         final String emailCC) throws ObjectNotFoundException, ValidationException {
        try {
            sendEmail(earlyAlert, emailCC);
        } catch (final SendFailedException e) {
            LOGGER.warn(
                    "Could not send Early Alert message to advisor.",
                    e);
            throw new ValidationException(
                    "Early Alert notification e-mail could not be sent to advisor. Early Alert was NOT created.",
                    e);
        }

    }

    private void sendEmail(@NotNull final EarlyAlert earlyAlert, // NOPMD
                           final String emailCC) throws SendFailedException,
            ObjectNotFoundException, ValidationException {
        if (earlyAlert == null) {
            throw new IllegalArgumentException("Early alert was missing.");
        }

        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert Person is missing.");
        }

        final Person person = earlyAlert.getPerson().getCoach();
        final Map<String, Object> messageParams = fillTemplateParametersUseCase.execute(earlyAlert);
        final SubjectAndBody subjAndBody = messageTemplateService
                .createEarlyAlertAdvisorConfirmationMessage(messageParams);

        Set<String> watcherEmailAddresses = new HashSet<String>(earlyAlert.getPerson().getWatcherEmailAddresses());
        if(emailCC != null && !emailCC.isEmpty())
        {
            watcherEmailAddresses.add(emailCC);
        }
        if ( person == null ) {
            LOGGER.warn("Student {} had no coach when EarlyAlert {} was"
                            + " created. Unable to send message to coach.",
                    earlyAlert.getPerson(), earlyAlert);
        } else {
            createAndQueueMessage(person, earlyAlert, watcherEmailAddresses, subjAndBody);
        }

        sendMessageToAllApplicableCampus(earlyAlert, subjAndBody);
    }

    private void createAndQueueMessage(
            final Person person,
            final EarlyAlert earlyAlert,
            final Set<String> watcherEmailAddresses,
            final SubjectAndBody subjAndBody
    ) throws ValidationException, ObjectNotFoundException {
        String emails = org.springframework.util.StringUtils.arrayToCommaDelimitedString(watcherEmailAddresses
                .toArray(new String[watcherEmailAddresses.size()]));
        final Message message = messageService.createMessage(person, emails, subjAndBody);
        LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
    }

    // Send same message to all applicable Campus Early Alert routing
    // entries
    private void sendMessageToAllApplicableCampus(
            EarlyAlert earlyAlert,
            SubjectAndBody subjAndBody
    ) throws ObjectNotFoundException, ValidationException {
        final PagingWrapper<EarlyAlertRouting> routes = earlyAlertRoutingService
                .getAllForCampus(earlyAlert.getCampus(), new SortingAndPaging(
                        ObjectStatus.ACTIVE));

        if (routes.getResults() > 0) {
            final ArrayList<String> alreadySent = Lists.newArrayList();

            for (final EarlyAlertRouting route : routes.getRows()) {
                checkThatRouteApplies(route);

                // Only routes that are for any of the Reasons in this EarlyAlert should be applied.
                if ( (earlyAlert.getEarlyAlertReasons() == null)
                        || !earlyAlert.getEarlyAlertReasons().contains(route.getEarlyAlertReason()) ) {
                    continue;
                }

                // Send e-mail to specific person
                final Person to = route.getPerson();
                if ( to != null && StringUtils.isNotBlank(to.getPrimaryEmailAddress()) ) {
                    //check if this alert has already been sent to this recipient, if so skip
                    if ( alreadySent.contains(route.getPerson().getPrimaryEmailAddress()) ) {
                        continue;
                    } else {
                        alreadySent.add(route.getPerson().getPrimaryEmailAddress());
                    }

                    final Message message = messageService.createMessage(to, null, subjAndBody);
                    LOGGER.info("Message {} for EarlyAlert {} also routed to {}",
                            new Object[]{message, earlyAlert, to}); // NOPMD
                }

                sendEmailToGroup(earlyAlert, subjAndBody, route);
            }
        }
    }

    private void sendEmailToGroup(EarlyAlert earlyAlert, SubjectAndBody subjAndBody, EarlyAlertRouting route) throws ObjectNotFoundException, ValidationException {
        if ( !StringUtils.isEmpty(route.getGroupName()) && !StringUtils.isEmpty(route.getGroupEmail()) ) {
            final Message message = messageService.createMessage(route.getGroupEmail(), null, subjAndBody);
            LOGGER.info("Message {} for EarlyAlert {} also routed to {}", new Object[] { message, earlyAlert, // NOPMD
                    route.getGroupEmail() });
        }
    }

    private void checkThatRouteApplies(EarlyAlertRouting route) throws ObjectNotFoundException {
        if ( route.getEarlyAlertReason() == null ) {
            throw new ObjectNotFoundException(
                    "EarlyAlertRouting missing EarlyAlertReason.", "EarlyAlertReason");
        }
    }
}
