package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.security.SspUser;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.SecurityService;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

class OpenEarlyAlertUseCase {

    private final EarlyAlertDao earlyAlertDao;
    private final SecurityService securityService;

    @Autowired
    public OpenEarlyAlertUseCase(EarlyAlertDao earlyAlertDao, SecurityService securityService) {
        this.earlyAlertDao = earlyAlertDao;
        this.securityService = securityService;
    }

    public void execute(UUID earlyAlertId) throws ObjectNotFoundException, ValidationException {
        final EarlyAlert earlyAlert = earlyAlertDao.get(earlyAlertId);

        // DAOs don't implement ObjectNotFoundException consistently and we'd
        // rather they not implement it at all, so a small attempt at 'future
        // proofing' here
        if ( earlyAlert == null ) {
            throw new ObjectNotFoundException(earlyAlertId, EarlyAlert.class.getName());
        }

        if ( earlyAlert.getClosedDate() == null ) {
            return;
        }

        final SspUser sspUser = securityService.currentUser();
        if ( sspUser == null ) {
            throw new ValidationException("Early Alert cannot be closed by a null User.");
        }

        earlyAlert.setClosedDate(null);
        earlyAlert.setClosedBy(null);

        // This save will result in a Hib session flush, which works fine with
        // our current usage. Future use cases might prefer to delay the
        // flush and we can address that when the time comes. Might not even
        // need to change anything here if it turns out nothing actually
        // *depends* on the flush.
        earlyAlertDao.save(earlyAlert);
    }
}
