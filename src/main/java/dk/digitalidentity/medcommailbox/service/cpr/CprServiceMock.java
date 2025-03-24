package dk.digitalidentity.medcommailbox.service.cpr;

import dk.digitalidentity.medcommailbox.service.cpr.dto.CprLookupDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Profile("CprServiceMock")
public class CprServiceMock extends AbstractCprService {

    private Random rand = new Random();

    private String maleNames[] = new String[] {"William","Oscar","Alfred","Oliver","Carl","Lucas","Noah","Valdemar","Malthe","Aksel","Elias","Emil","Arthur","August","Magnus","Victor","Viggo","Felix","Anton","Alexander","Elliot","Frederik","Nohr","Lauge","Liam","Theo","Otto","Hugo","Adam","Loui","Theodor","Villads","Johan","Storm 	","Albert","Villum","Christian","Milas","Konrad","Mads","Mikkel","Benjamin","Pelle","Erik","Marius","Anker","Asger","Matheo","Magne","Sebastian"};
    private String femaleNames[] = new String[] {"Alma","Agnes","Ella","Freja","Clara","Emma","Sofia","Karla","Anna","Ellie","Olivia","Alberte","Nora","Asta","Laura","Josefine","Ida","Luna","Frida","Lily","Ellen","Mathilde","Astrid","Isabella","Aya","Esther","Lærke","Maja","Emily","Andrea","Victoria","Liva","Marie","Vilma","Saga","Mille","Sofie","Emilie","Leonora","Rosa","Liv","Merle","Hannah","Lea","Molly","Alba","Sara","Gry","Alva","Johanne"};
    private String surnames[] = new String[] {"Nielsen","Jensen","Hansen","Andersen","Pedersen","Christensen","Larsen","Sørensen","Rasmussen","Jørgensen","Petersen","Madsen","Kristensen","Olsen","Thomsen","Christiansen","Poulsen","Johansen","Møller","Mortensen"};

    private String getRandomFirstname(String cpr)
    {
        var isFemale = true;
        try {
            isFemale = Integer.parseInt(cpr.substring(9,10)) % 2 == 0;
        } catch (Exception e) {
        }
        var randomName = isFemale ? femaleNames[rand.nextInt(femaleNames.length)] : maleNames[rand.nextInt(maleNames.length)];
        return randomName + " Fiktiv";
    }

    private String getRandomSurname()
    {
        return surnames[rand.nextInt(surnames.length)];
    }

    @Override
    public CprLookupDto cprLookup(String cpr) {
        if (!validCpr(cpr)) {
            return null;
        }
        CprLookupDto dto = new CprLookupDto();
        dto.setName(getRandomFirstname(cpr));
        dto.setSurname(getRandomSurname());
        dto.setBirthday(getBirthDateFromCpr(cpr));
        dto.setCity("Testby");
        dto.setPostCode("9999");
        dto.setAddress("Testvej 1");
        dto.setCpr(cpr);
        return dto;
    }

}
