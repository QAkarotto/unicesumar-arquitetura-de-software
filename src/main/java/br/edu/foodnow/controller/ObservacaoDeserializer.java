package br.edu.foodnow.controller;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Impede coerção de números e booleanos apenas no novo campo textual. */
public class ObservacaoDeserializer extends ValueDeserializer<String> {
    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            return parser.getString();
        }
        return context.reportInputMismatch(String.class, "observacao deve ser uma string ou null");
    }
}
