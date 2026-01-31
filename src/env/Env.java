package src.env;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

// Environment code for project robotsOnMars
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.parser.ParseException;
import jason.environment.Environment;
import src.view.AppFrame;
import src.model.*;

public class Env extends Environment {

    private Logger logger = Logger.getLogger("robotsOnMars." + Env.class.getName());

    private Mars mars = new Mars(100, 0.05, 0.005, 0.01, 5);

    /** Called before the MAS execution with the args informed in .mas2j */
    @Override
    public void init(String[] args) {
        super.init(args);
        try {
            addPercept(ASSyntax.parseLiteral("percept(" + args[0] + ")"));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new AppFrame(mars).setVisible(true);
        });
    }

    @Override
    public boolean executeAction(String agName, Structure action) {
        logger.info("executing: " + action + ", but not implemented!");
        if (true) { // you may improve this condition
            informAgsEnvironmentChanged();
        }
        return true; // the action was executed with success
    }

    /** Called before the end of MAS execution */
    @Override
    public void stop() {
        super.stop();
    }
}
