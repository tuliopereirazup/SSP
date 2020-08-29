package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.reference.EarlyAlertReason;
import org.jasig.ssp.model.reference.EarlyAlertSuggestion;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.EarlyAlertReasonService;
import org.jasig.ssp.service.reference.EarlyAlertSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

class SaveEarlyAlertUseCase {

    private final EarlyAlertDao earlyAlertDao;
    private final PersonService personService;
    private final EarlyAlertReasonService earlyAlertReasonService;
    private final EarlyAlertSuggestionService earlyAlertSuggestionService;

    @Autowired
    public SaveEarlyAlertUseCase(
            EarlyAlertDao dao,
            PersonService personService,
            EarlyAlertReasonService earlyAlertReasonService,
            EarlyAlertSuggestionService earlyAlertSuggestionService) {
        this.earlyAlertDao = dao;
        this.personService = personService;
        this.earlyAlertReasonService = earlyAlertReasonService;
        this.earlyAlertSuggestionService = earlyAlertSuggestionService;
    }

    public EarlyAlert execute(@NotNull final EarlyAlert obj) throws ObjectNotFoundException {
        final EarlyAlert current = earlyAlertDao.get(obj.getId());

        current.setCourseName(obj.getCourseName());
        current.setCourseTitle(obj.getCourseTitle());
        current.setEmailCC(obj.getEmailCC());
        current.setCampus(obj.getCampus());
        current.setEarlyAlertReasonOtherDescription(obj
                .getEarlyAlertReasonOtherDescription());
        current.setComment(obj.getComment());
        current.setClosedDate(obj.getClosedDate());
        setClosedBy(obj, current);
        setPerson(obj, current);
        current.setEarlyAlertReasons(createEarlyAlertReasons(obj));
        current.setEarlyAlertSuggestions(createEarlyAlertSuggestions(obj));

        return earlyAlertDao.save(current);
    }

    private void setPerson(EarlyAlert obj, EarlyAlert current) throws ObjectNotFoundException {
        if (obj.getPerson() == null) {
            current.setPerson(null);
        } else {
            current.setPerson(personService.get(obj.getPerson().getId()));
        }
    }

    private void setClosedBy(EarlyAlert obj, EarlyAlert current) throws ObjectNotFoundException {
        if ( obj.getClosedById() == null ) {
            current.setClosedBy(null);
        } else {
            current.setClosedBy(personService.get(obj.getClosedById()));
        }
    }

    private Set<EarlyAlertReason> createEarlyAlertReasons(EarlyAlert obj) {
        final Set<EarlyAlertReason> earlyAlertReasons = new HashSet<EarlyAlertReason>();
        if (obj.getEarlyAlertReasons() != null) {
            for (final EarlyAlertReason reason : obj.getEarlyAlertReasons()) {
                earlyAlertReasons.add(earlyAlertReasonService.load(reason.getId()));
            }
        }
        return earlyAlertReasons;
    }

    private Set<EarlyAlertSuggestion> createEarlyAlertSuggestions(EarlyAlert obj) {
        final Set<EarlyAlertSuggestion> earlyAlertSuggestions = new HashSet<EarlyAlertSuggestion>();
        if (obj.getEarlyAlertSuggestions() != null) {
            for (final EarlyAlertSuggestion reason : obj.getEarlyAlertSuggestions()) {
                earlyAlertSuggestions.add(earlyAlertSuggestionService.load(reason.getId()));
            }
        }
        return earlyAlertSuggestions;
    }
}
