package org.jasig.ssp.service.impl;

import org.jasig.ssp.model.JournalEntry;
import org.jasig.ssp.model.JournalEntryDetail;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonProgramStatusService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;

class JournalEntryTransactionCheckUseCase {

    private final PersonProgramStatusService personProgramStatusService;

    @Autowired
    public JournalEntryTransactionCheckUseCase(PersonProgramStatusService personProgramStatusService) {
        this.personProgramStatusService = personProgramStatusService;
    }

    public void execute(final JournalEntry journalEntry) throws ObjectNotFoundException, ValidationException {
        for (final JournalEntryDetail detail : journalEntry.getJournalEntryDetails()) {
            if (detail.getJournalStepJournalStepDetail().getJournalStep().isUsedForTransition()) {
                // is used for transition, so attempt to set program status
                personProgramStatusService.setTransitionForStudent(journalEntry.getPerson());
                // exit early because no need to loop through others
                return;
            }
        }
    }
}
