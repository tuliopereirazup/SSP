package org.jasig.ssp.service.impl;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.Person;
import org.jasig.ssp.model.external.FacultyCourse;
import org.jasig.ssp.model.external.Term;
import org.jasig.ssp.model.reference.EnrollmentStatus;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.external.FacultyCourseService;
import org.jasig.ssp.service.external.TermService;
import org.jasig.ssp.service.reference.ConfigService;
import org.jasig.ssp.service.reference.EnrollmentStatusService;
import org.jasig.ssp.transferobject.messagetemplate.CoachPersonLiteMessageTemplateTO;
import org.jasig.ssp.transferobject.messagetemplate.EarlyAlertMessageTemplateTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.util.Map;

class FillTemplateParametersUseCase {

    private final ConfigService configService;
    private final PersonService personService;
    private final FacultyCourseService facultyCourseService;
    private final TermService termService;
    private final EnrollmentStatusService enrollmentStatusService;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertServiceImpl.class);

    @Autowired
    public FillTemplateParametersUseCase(
        ConfigService configService,
        PersonService personService,
        FacultyCourseService facultyCourseService,
        TermService termService,
        EnrollmentStatusService enrollmentStatusService
    ) {
        this.configService = configService;
        this.personService = personService;
        this.facultyCourseService = facultyCourseService;
        this.termService = termService;
        this.enrollmentStatusService = enrollmentStatusService;
    }

    public Map<String, Object> execute(@NotNull final EarlyAlert earlyAlert) {
        requireValidParameters(earlyAlert);

        final Map<String, Object> templateParameters = Maps.newHashMap();

        addParametersIfCourseNameIsValid(earlyAlert, templateParameters);

        Person creator = null;
        try{
            creator = personService.get(earlyAlert.getCreatedBy().getId());
        }catch(ObjectNotFoundException exp)	{
            LOGGER.error("Early Alert Creator Not found sending message for early alert:" + earlyAlert.getId(), exp);
        }

        EarlyAlertMessageTemplateTO eaMTO = new EarlyAlertMessageTemplateTO(earlyAlert, creator);

        addCoachIfApplies(earlyAlert, eaMTO);

        addEnrollmentIfApplies(templateParameters, eaMTO);

        addDefaultParameters(templateParameters, eaMTO);

        return templateParameters;
    }

    private void requireValidParameters(EarlyAlert earlyAlert) {
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert was missing.");
        }

        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert.Person is missing.");
        }

        if (earlyAlert.getCreatedBy() == null) {
            throw new IllegalArgumentException(
                    "EarlyAlert.CreatedBy is missing.");
        }

        if (earlyAlert.getCampus() == null) {
            throw new IllegalArgumentException("EarlyAlert.Campus is missing.");
        }

        // ensure earlyAlert.createdBy is populated
        if ((earlyAlert.getCreatedBy() == null)
                || (earlyAlert.getCreatedBy().getFirstName() == null)) {
            if (earlyAlert.getCreatedBy() == null) {
                throw new IllegalArgumentException(
                        "EarlyAlert.CreatedBy is missing.");
            }
        }
    }

    private void addCoachIfApplies(EarlyAlert earlyAlert, EarlyAlertMessageTemplateTO eaMTO) {
        //Only early alerts response late messages sent to coaches
        if(eaMTO.getCoach() == null){
            try{
                // if no earlyAlert.getCampus()  error thrown by design, should never not be a campus.
                eaMTO.setCoach(new CoachPersonLiteMessageTemplateTO(personService.get(earlyAlert.getCampus().getEarlyAlertCoordinatorId())));
            }catch(ObjectNotFoundException exp){
                LOGGER.error("Early Alert with id: " + earlyAlert.getId() + " does not have valid campus coordinator, no coach assigned: " + earlyAlert.getCampus().getEarlyAlertCoordinatorId(), exp);
            }
        }
    }

    private void addEnrollmentIfApplies(Map<String, Object> templateParameters, EarlyAlertMessageTemplateTO eaMTO) {
        String statusCode = eaMTO.getEnrollmentStatus();
        if(statusCode != null) {
            EnrollmentStatus enrollmentStatus = enrollmentStatusService.getByCode(statusCode);
            if(enrollmentStatus != null) {
                //if we have made it here... we can add the status!
                templateParameters.put("enrollment", enrollmentStatus);
            }
        }
    }

    private void addParametersIfCourseNameIsValid(EarlyAlert earlyAlert, Map<String, Object> templateParameters) {
        final String courseName = earlyAlert.getCourseName();
        if ( StringUtils.isNotBlank(courseName) ) {
            Person creator;
            try {
                creator = personService.get(earlyAlert.getCreatedBy().getId());
            } catch (ObjectNotFoundException e1) {
                throw new IllegalArgumentException(
                        "EarlyAlert.CreatedBy.Id could not be loaded.", e1);
            }
            final String facultySchoolId = creator.getSchoolId();
            if ( (StringUtils.isNotBlank(facultySchoolId)) ) {
                String termCode = earlyAlert.getCourseTermCode();
                FacultyCourse course = getCourse(courseName, facultySchoolId, termCode);
                if ( course != null ) {
                    addParametetrsRelatedToCourse(earlyAlert, templateParameters, termCode, course);
                } else {
                    LOGGER.info("Not adding course nor term to message template"
                                    + " params for early alert {} because the associated"
                                    + " course {} and faculty school id {} did not"
                                    + " resolve to an external course record.",
                            new Object[] { earlyAlert.getId(), courseName,
                                    facultySchoolId});
                }
            }
        }
    }

    private void addParametetrsRelatedToCourse(EarlyAlert earlyAlert, Map<String, Object> templateParameters, String termCode, FacultyCourse course) {
        templateParameters.put("course", course);
        if ( StringUtils.isBlank(termCode) ) {
            termCode = course.getTermCode();
        }
        if ( StringUtils.isNotBlank(termCode) ) {
            Term term = null;
            try {
                term = termService.getByCode(termCode);
            } catch ( ObjectNotFoundException e ) {
                // Trace irrelevant. See below for logging.
            }
            if ( term != null ) {
                templateParameters.put("term", term);
            } else {
                LOGGER.info("Not adding term to message template"
                                + " params or early alert {} because"
                                + " the term code {} did not resolve to"
                                + " an external term record",
                        earlyAlert.getId(), termCode);
            }
        }
    }

    private FacultyCourse getCourse(String courseName, String facultySchoolId, String termCode) {
        try {
            if ( StringUtils.isBlank(termCode) ) {
                return facultyCourseService.
                        getCourseByFacultySchoolIdAndFormattedCourse(
                                facultySchoolId, courseName);
            } else {
                return facultyCourseService.
                        getCourseByFacultySchoolIdAndFormattedCourseAndTermCode(
                                facultySchoolId, courseName, termCode);
            }
        } catch ( ObjectNotFoundException e ) {
            // Trace irrelevant. see below for logging. prefer to
            // do it there, after the null check b/c not all service
            // methods implement ObjectNotFoundException reliably.
            return null;
        }
    }

    private void addDefaultParameters(Map<String, Object> templateParameters, EarlyAlertMessageTemplateTO eaMTO) {
        templateParameters.put("earlyAlert", eaMTO);
        templateParameters.put("termToRepresentEarlyAlert",
                configService.getByNameEmpty("term_to_represent_early_alert"));
        templateParameters.put("TermToRepresentEarlyAlert",
                configService.getByNameEmpty("term_to_represent_early_alert"));
        templateParameters.put("termForEarlyAlert",
                configService.getByNameEmpty("term_to_represent_early_alert"));
        templateParameters.put("linkToSSP",
                configService.getByNameEmpty("serverExternalPath"));
        templateParameters.put("applicationTitle",
                configService.getByNameEmpty("app_title"));
        templateParameters.put("institutionName",
                configService.getByNameEmpty("inst_name"));

        templateParameters.put("FirstName", eaMTO.getPerson().getFirstName());
        templateParameters.put("LastName", eaMTO.getPerson().getLastName());
        templateParameters.put("CourseName", eaMTO.getCourseName());
    }
}
