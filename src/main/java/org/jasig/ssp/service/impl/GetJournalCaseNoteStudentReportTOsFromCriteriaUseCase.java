package org.jasig.ssp.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.dao.JournalEntryDao;
import org.jasig.ssp.dao.PersonDao;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.reports.BaseStudentReportTO;
import org.jasig.ssp.transferobject.reports.JournalCaseNotesStudentReportTO;
import org.jasig.ssp.transferobject.reports.JournalStepSearchFormTO;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GetJournalCaseNoteStudentReportTOsFromCriteriaUseCase {

    private final PersonDao personDao;
    private final JournalEntryDao journalEntryDao;

    @Autowired
    public GetJournalCaseNoteStudentReportTOsFromCriteriaUseCase(
        PersonDao personDao,
        JournalEntryDao journalEntryDao
    ) {
        this.personDao = personDao;
        this.journalEntryDao = journalEntryDao;
    }

    public List<JournalCaseNotesStudentReportTO> execute(
        JournalStepSearchFormTO personSearchForm,
        SortingAndPaging sAndP
    ) throws ObjectNotFoundException {
        final List<JournalCaseNotesStudentReportTO> personsWithJournalEntries = journalEntryDao.getJournalCaseNoteStudentReportTOsFromCriteria(personSearchForm, sAndP);
        final Map<String, JournalCaseNotesStudentReportTO> map = new HashMap<String, JournalCaseNotesStudentReportTO>();

        for(JournalCaseNotesStudentReportTO entry:personsWithJournalEntries){
            map.put(entry.getSchoolId(), entry);
        }

        final SortingAndPaging personSAndP = SortingAndPaging.createForSingleSortAll(ObjectStatus.ACTIVE, "lastName", "DESC") ;
        final PagingWrapper<BaseStudentReportTO> persons = personDao.getStudentReportTOs(personSearchForm, personSAndP);

        if (persons == null) {
            return personsWithJournalEntries;
        }

        filterPersonsWithJournalEntries(personSearchForm, personsWithJournalEntries, map, persons);

        personsWithJournalEntries.sort(this::compareTwoStudentsByName);

        return personsWithJournalEntries;
    }

    private void filterPersonsWithJournalEntries(
        JournalStepSearchFormTO personSearchForm,
        List<JournalCaseNotesStudentReportTO> personsWithJournalEntries,
        Map<String, JournalCaseNotesStudentReportTO> map,
        PagingWrapper<BaseStudentReportTO> persons
    ) {
        for (BaseStudentReportTO person: persons) {
            if (!map.containsKey(person.getSchoolId()) && StringUtils.isNotBlank(person.getCoachSchoolId())) {
                boolean addStudent = true;
                if (personSearchForm.getJournalSourceIds() != null) {
                    if (journalEntryDao.getJournalCountForPersonForJournalSourceIds(person.getId(),
                        personSearchForm.getJournalSourceIds()) == 0) {
                        addStudent = false;
                    }
                }
                if (addStudent) {
                    final JournalCaseNotesStudentReportTO entry = new JournalCaseNotesStudentReportTO(person);
                    personsWithJournalEntries.add(entry);
                    map.put(entry.getSchoolId(), entry);
                }
            }
        }
    }

    private int compareTwoStudentsByName(
        JournalCaseNotesStudentReportTO p1,
        JournalCaseNotesStudentReportTO p2
    ) {
        int value = p1.getLastName().compareToIgnoreCase(
                p2.getLastName());
        if(value != 0)
            return value;

        value = p1.getFirstName().compareToIgnoreCase(
                p2.getFirstName());
        if(value != 0)
            return value;
        if(p1.getMiddleName() == null && p2.getMiddleName() == null)
            return 0;
        if(p1.getMiddleName() == null)
            return -1;
        if(p2.getMiddleName() == null)
            return 1;
        return p1.getMiddleName().compareToIgnoreCase(
                p2.getMiddleName());
    }
}
