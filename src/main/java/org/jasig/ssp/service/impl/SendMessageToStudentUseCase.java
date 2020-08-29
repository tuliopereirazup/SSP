package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.Message;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.mail.SendFailedException;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class SendMessageToStudentUseCase {

    private final MessageTemplateService messageTemplateService;
    private final FillTemplateParametersUseCase fillTemplateParametersUseCase;
    private final MessageService messageService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertServiceImpl.class);

    @Autowired
    public SendMessageToStudentUseCase(
        MessageTemplateService messageTemplateService,
        FillTemplateParametersUseCase fillTemplateParametersUseCase,
        MessageService messageService
    ) {
        this.messageTemplateService = messageTemplateService;
        this.fillTemplateParametersUseCase = fillTemplateParametersUseCase;
        this.messageService = messageService;
    }

    public void execute(@NotNull final EarlyAlert earlyAlert)
            throws ObjectNotFoundException, SendFailedException, ValidationException {
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert was missing.");
        }

        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert.Person is missing.");
        }

        final Person person = earlyAlert.getPerson();
        final Map<String, Object> messageParams = fillTemplateParametersUseCase.execute(earlyAlert);
        final SubjectAndBody subjAndBody = messageTemplateService.createEarlyAlertToStudentMessage(messageParams);

        createAndQueueMessage(earlyAlert, person, subjAndBody);
    }

    private void createAndQueueMessage(EarlyAlert earlyAlert, Person person, SubjectAndBody subjAndBody)
            throws ObjectNotFoundException, ValidationException {
        Set<String> watcherEmails = new HashSet<String>(person.getWatcherEmailAddresses());
        final String emails = StringUtils.arrayToCommaDelimitedString(watcherEmails
            .toArray(new String[watcherEmails.size()]));
        final Message message = messageService.createMessage(person, emails, subjAndBody);

        LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
    }
}
