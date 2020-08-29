package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.*;
import org.jasig.ssp.model.reference.ProgramStatus;
import org.jasig.ssp.model.reference.StudentType;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonProgramStatusService;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.ProgramStatusService;
import org.jasig.ssp.service.reference.StudentTypeService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.util.*;

class EarlyAlertCreatorUseCase {

    private final PersonProgramStatusService personProgramStatusService;
    private final StudentTypeService studentTypeService;
    private final ProgramStatusService programStatusService;
    private final EarlyAlertAdvisorMessageSenderUseCase assignedAdvisorEmailSender;
    private final EarlyAlertFacultyConfirmationSenderUseCase facultyMessageSender;
    private final PersonService personService;
    private final EarlyAlertDao earlyAlertDao;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertServiceImpl.class);

    @Autowired
    public EarlyAlertCreatorUseCase(
            PersonProgramStatusService personProgramStatusService,
            StudentTypeService studentTypeService,
            ProgramStatusService programStatusService,
            EarlyAlertAdvisorMessageSenderUseCase AssignedAdvisorEmailSender,
            EarlyAlertFacultyConfirmationSenderUseCase facultyMessageSender,
            PersonService personService,
            EarlyAlertDao earlyAlertDao
    ) {
        this.personProgramStatusService = personProgramStatusService;
        this.studentTypeService = studentTypeService;
        this.programStatusService = programStatusService;
        this.assignedAdvisorEmailSender = AssignedAdvisorEmailSender;
        this.facultyMessageSender = facultyMessageSender;
        this.personService = personService;
        this.earlyAlertDao = earlyAlertDao;
    }

    public EarlyAlert execute(@NotNull final EarlyAlert earlyAlert)
            throws ObjectNotFoundException, ValidationException {
        requireValidParameters(earlyAlert);

        final Person student = earlyAlert.getPerson();

        final UUID assignedAdvisor = getEarlyAlertAdvisor(earlyAlert);

        checkIfIsValidEarlyAdvisorAlert(assignedAdvisor, student);

        if (student.getCoach() == null
                || assignedAdvisor.equals(student.getCoach().getId())) {
            student.setCoach(personService.get(assignedAdvisor));
        }

        ensureValidAlertedOnPersonStateNoFail(student);

        return createAlert(earlyAlert);
    }

    private void requireValidParameters(EarlyAlert earlyAlert) throws ValidationException {
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert must be provided.");
        }

        if (earlyAlert.getPerson() == null) {
            throw new ValidationException(
                    "EarlyAlert Student data must be provided.");
        }
    }

    private void checkIfIsValidEarlyAdvisorAlert(final UUID assignedAdvisor, final Person student)
            throws ValidationException {
        if (assignedAdvisor == null) {
            throw new ValidationException(
                    "Could not determine the Early Alert Advisor for student ID "
                            + student.getId());
        }
    }

    private EarlyAlert createAlert(final EarlyAlert earlyAlert) throws ValidationException, ObjectNotFoundException {
        final EarlyAlert saved = earlyAlertDao.save(earlyAlert);
        assignedAdvisorEmailSender.execute(saved, earlyAlert.getEmailCC());
        facultyMessageSender.execute(earlyAlert);
        return saved;
    }

    /**
     * Business logic to determine the advisor that is assigned to the student
     * for this Early Alert.
     *
     * @param earlyAlert
     *            EarlyAlert instance
     * @throws ValidationException
     *             If Early Alert, Student, and/or system information could not
     *             determine the advisor for this student.
     * @return The assigned advisor
     */
    private UUID getEarlyAlertAdvisor(final EarlyAlert earlyAlert)
            throws ValidationException {
        // Check for student already assigned to an advisor (a.k.a. coach)
        if ((earlyAlert.getPerson().getCoach() != null) &&
                (earlyAlert.getPerson().getCoach().getId() != null)) {
            return earlyAlert.getPerson().getCoach().getId();
        }

        // Get campus Early Alert coordinator
        if (earlyAlert.getCampus() == null) {
            throw new IllegalArgumentException("Campus ID can not be null.");
        }

        if (earlyAlert.getCampus().getEarlyAlertCoordinatorId() != null) {
            // Return Early Alert coordinator UUID
            return earlyAlert.getCampus().getEarlyAlertCoordinatorId();
        }

        // TODO If no campus EA Coordinator, assign to default EA Coordinator
        // (which is not yet implemented)

        // getEarlyAlertAdvisor should never return null
        throw new ValidationException(
                "Could not determined the Early Alert Coordinator for this student. Ensure that a default coordinator is set globally and for all campuses.");
    }

    private void ensureValidAlertedOnPersonStateNoFail(Person person) {
        try {
            ensureValidAlertedOnPersonStateOrFail(person);
        } catch ( Exception e ) {
            LOGGER.error("Unable to set a program status or student type on "
                    + "person '{}'. This is likely to prevent that person "
                    + "record from appearing in caseloads, student searches, "
                    + "and some reports.", person.getId(), e);
        }
    }

    private void ensureValidAlertedOnPersonStateOrFail(Person person)
            throws ObjectNotFoundException, ValidationException {

        if ( person.getObjectStatus() != ObjectStatus.ACTIVE ) {
            person.setObjectStatus(ObjectStatus.ACTIVE);
        }

        final ProgramStatus programStatus =  programStatusService.getActiveStatus();
        if ( programStatus == null ) {
            throw new ObjectNotFoundException(
                    "Unable to find a ProgramStatus representing \"activeness\".",
                    "ProgramStatus");
        }

        Set<PersonProgramStatus> programStatuses =
                person.getProgramStatuses();
        if ( programStatuses == null || programStatuses.isEmpty() ) {
            PersonProgramStatus personProgramStatus = new PersonProgramStatus();
            personProgramStatus.setEffectiveDate(new Date());
            personProgramStatus.setProgramStatus(programStatus);
            personProgramStatus.setPerson(person);
            programStatuses.add(personProgramStatus);
            person.setProgramStatuses(programStatuses);
            // save should cascade, but make sure custom create logic fires
            personProgramStatusService.create(personProgramStatus);
        }

        if ( person.getStudentType() == null ) {
            StudentType studentType = studentTypeService.get(StudentType.EAL_ID);
            if ( studentType == null ) {
                throw new ObjectNotFoundException(
                        "Unable to find a StudentType representing an early "
                                + "alert-assigned type.", "StudentType");
            }
            person.setStudentType(studentType);
        }
    }
}
