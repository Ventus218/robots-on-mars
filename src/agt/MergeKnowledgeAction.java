package src.agt;

import java.util.*;
import jason.asSemantics.*;
import jason.asSyntax.*;
import src.env.Env;
import src.model.*;

public class MergeKnowledgeAction extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (!(args[0] instanceof MapTerm)) {
            throw new IllegalArgumentException("Argument 0 must be a MapTerm");
        }
        MapTerm map = (MapTerm) args[0];

        if (!args[1].isList()) {
            throw new IllegalArgumentException("Argument 1 must be a ListTerm");
        }
        ListTerm newCells = (ListTerm) args[1];

        final Mars mars = Env.instance().mars();
        final HasViewOfMars ag;
        final var roverOpt = mars.rover(ts.getAgArch().getAgName());
        if (roverOpt.isPresent()) {
            ag = roverOpt.get();
        } else {
            ag = mars.base();
        }

        final Map<Coordinates, Terrain> updates = new HashMap<>();
        for (final var newCellTerm : newCells) {
            // cell(Coord, Terrain, Timestamp)
            Structure newCell = (Structure) newCellTerm;

            Term coord = newCell.getTerm(0);
            Term terrain = newCell.getTerm(1);
            Term timestamp = newCell.getTerm(2);
            long timestampLong = Env.Lit.toLong(timestamp);

            // data(Terrain, Timestamp)
            Structure knownCell = (Structure) map.get(coord);

            if (knownCell == null || Env.Lit.toLong(knownCell.getTerm(1)) < timestampLong) {
                Structure newData = new Structure("data");
                newData.addTerm(terrain);
                newData.addTerm(timestamp);

                map.put(coord, newData);

                updates.put(Env.Lit.toCoordinates(coord), Env.Lit.toTerrain(terrain));
            }
        }
        if (!updates.isEmpty()) {
            mars.updateMarsViewOf(ag, updates);
        }
        return true;
    }
}
