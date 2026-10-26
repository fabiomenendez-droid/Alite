package de.phbouillon.android.games.alite;

import de.phbouillon.android.games.alite.model.generator.GalaxyGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

public class GalaxyRegressionTest {
    @Test public void originalFirstGalaxyAndLaveArePreserved() {
        GalaxyGenerator galaxy = new GalaxyGenerator();
        galaxy.buildGalaxy(1);
        assertEquals(256, galaxy.getSystems().length);
        assertEquals("Lave", galaxy.getSystem(7).getName());
        assertEquals(1, galaxy.getCurrentGalaxy());
    }

    @Test public void allEightGalaxiesRemainDeterministic() {
        GalaxyGenerator galaxy = new GalaxyGenerator();
        for (int number = 1; number <= 8; number++) {
            galaxy.buildGalaxy(number);
            String[] names = new String[256];
            for (int i = 0; i < names.length; i++) names[i] = galaxy.getSystem(i).getName();
            galaxy.buildGalaxy(number);
            for (int i = 0; i < names.length; i++) assertEquals(names[i], galaxy.getSystem(i).getName());
            assertEquals(number, galaxy.getCurrentGalaxy());
        }
    }
}
