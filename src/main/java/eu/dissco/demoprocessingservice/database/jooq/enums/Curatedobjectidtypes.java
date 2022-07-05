/*
 * This file is generated by jOOQ.
 */
package eu.dissco.demoprocessingservice.database.jooq.enums;


import eu.dissco.demoprocessingservice.database.jooq.Public;

import org.jooq.Catalog;
import org.jooq.EnumType;
import org.jooq.Schema;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public enum Curatedobjectidtypes implements EnumType {

    physicalSpecimenID("physicalSpecimenID"),

    CETAFID("CETAFID");

    private final String literal;

    private Curatedobjectidtypes(String literal) {
        this.literal = literal;
    }

    @Override
    public Catalog getCatalog() {
        return getSchema().getCatalog();
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public String getName() {
        return "curatedobjectidtypes";
    }

    @Override
    public String getLiteral() {
        return literal;
    }
}
