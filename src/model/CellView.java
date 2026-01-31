package src.model;

import java.util.Date;

sealed interface CellView permits Known, Unknown {
}

record Known(Cell cell, Date timestamp) implements CellView {
}

record Unknown() implements CellView {
}
