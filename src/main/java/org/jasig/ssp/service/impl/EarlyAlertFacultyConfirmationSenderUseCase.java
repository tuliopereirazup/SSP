package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.Message;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.SubjectAndBody;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.MessageTemplateService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.SendFailedException;
import java.util.Map;
import java.util.UUID;

class EarlyAlertFacultyConfirmationSenderUseCase {

    private final ConfigService configService;
    private final PersonService personService;
    private final MessageTemplateService messageTemplateService;
    private final MessageService messageService;
    private final FillTemplateParametersUseCase fillTemplateParametersUseCase;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertServiceImpl.class);

    @Autowired
    public EarlyAlertFacultyConfirmationSenderUseCase(
        ConfigService configService,
        PersonService personService,
        MessageTemplateService messageTemplateService,
        MessageService messageService,
        FillTemplateParametersUseCase fillTemplateParametersUseCase
    ) {
        this.configService = configService;
        this.personService = personService;
        this.messageTemplateService = messageTemplateService;
        this.messageService = messageService;
        this.fillTemplateParametersUseCase = fillTemplateParametersUseCase;
    }

    /**
     * Send confirmation e-mail ({@link Message}) to the faculty who created
     * this alert.
     *
     * @param earlyAlert
     *            Early Alert
     * @throws ObjectNotFoundException
     * @throws ValidationException
     */
    public void execute(final EarlyAlert earlyAlert) throws ValidationException, ObjectNotFoundException {
        try {
            sendMessage(earlyAlert);
        } catch (final SendFailedException e) {
            LOGGER.warn(
                    "Could not send Early Alert confirmation to faculty.",
                    e);
            throw new ValidationException(
                    "Early Alert confirmation e-mail could not be sent. Early Alert was NOT created.",
                    e);
        }
    }

    private void sendMessage(final EarlyAlert earlyAlert) throws SendFailedException,
            ObjectNotFoundException, ValidationException {
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert was missing.");
        }

        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert.Person is missing.");
        }

        if (!configService.getByNameOrDefaultValue("send_faculty_mail")) {
            LOGGER.debug("Skipping Faculty Early Alert Confirmation Email: Config Turned Off");
            return; //skip if faculty early alert email turned off
        }

        final UUID personId = earlyAlert.getCreatedBy().getId();
        Person person = personService.get(personId);
        if ( person == null ) {
            LOGGER.warn("EarlyAlert {} has no creator. Unable to send"
                    + " confirmation message to faculty.", earlyAlert);
        } else {
            createAndQueueMessage(earlyAlert, person);
        }
    }

    private void createAndQueueMessage(EarlyAlert earlyAlert, Person person)
            throws ObjectNotFoundException, ValidationException {
        final Map<String, Object> messageParams = fillTemplateParametersUseCase.execute(earlyAlert);
        final SubjectAndBody subjAndBody = messageTemplateService
                .createEarlyAlertFacultyConfirmationMessage(messageParams);

        final Message message = messageService.createMessage(person, null,
                subjAndBody);

        LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
    }
}
