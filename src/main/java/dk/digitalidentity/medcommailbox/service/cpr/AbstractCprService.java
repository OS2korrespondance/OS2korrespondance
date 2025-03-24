package dk.digitalidentity.medcommailbox.service.cpr;



import dk.digitalidentity.medcommailbox.config.MedcomMailboxConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public abstract class AbstractCprService implements ICprService {

    @Autowired
    protected MedcomMailboxConfiguration settings;

    protected LocalDate getBirthDateFromCpr(String cpr) {
        var datePart = Integer.parseInt(cpr.substring(0, 2));
        var monthPart = Integer.parseInt(cpr.substring(2, 4));
        var yearPart = Integer.parseInt(cpr.substring(4, 6));
        var seventh = Integer.parseInt(cpr.substring(6, 7));
        var century = 0;
        if (seventh < 4) {
            century = 1900;
        } else if (seventh == 4 || seventh == 9) {
            century = yearPart < 37 ? 2000 : 1900;
        } else {
            century = yearPart < 58 ? 2000 : 1800;
        }
        return LocalDate.of(century + yearPart, monthPart, datePart);
    }

    @Override
    public boolean validCpr(String cpr) {
        if (cpr == null || cpr.length() != 10) {
            return false;
        }

        for (char c : cpr.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }

        int days = Integer.parseInt(cpr.substring(0, 2));
        int month = Integer.parseInt(cpr.substring(2, 4));

        if (days < 1 || days > 31) {
            return false;
        }

        if (month < 1 || month > 12) {
            return false;
        }

        return true;
    }
}