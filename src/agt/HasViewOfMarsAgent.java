package src.agt;

import java.util.List;

import jason.RevisionFailedException;
import jason.asSemantics.*;
import jason.asSyntax.Literal;
import src.env.Env;
import src.model.HasViewOfMars;
import src.model.Mars;

public class HasViewOfMarsAgent extends Agent {
    private HasViewOfMars _view = null;

    private Mars mars() {
        return Env.instance().mars();
    }

    private HasViewOfMars view() {
        if (_view == null) {
            final var name = this.getTS().getAgArch().getAgName();
            if (name.equals("base")) {
                _view = mars().base();
            } else {
                _view = mars().rover(name).get();
            }
        }
        return _view;
    }

    @Override
    public List<Literal>[] brf(Literal beliefToAdd, Literal beliefToDel, Intention i, boolean addEnd)
            throws RevisionFailedException {
        if (beliefToAdd != null && beliefToAdd.getFunctor().equals("cell")) {
            final var coord = Env.Lit.toCoordinates(beliefToAdd.getTerm(0));
            final var terrain = Env.Lit.toTerrain(beliefToAdd.getTerm(1));
            mars().updateMarsViewOf(view(), coord, terrain);
        }
        return super.brf(beliefToAdd, beliefToDel, i, addEnd);
    }
}
