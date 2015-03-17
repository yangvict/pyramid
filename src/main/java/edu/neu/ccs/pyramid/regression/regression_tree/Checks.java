package edu.neu.ccs.pyramid.regression.regression_tree;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chengli on 10/28/14.
 */
@JsonSerialize(using = Checks.Serializer.class)
class Checks {
    List<Integer> featureIndices;
    List<String> featureNames;
    List<Double> thresholds;
    //true=go left
    List<Boolean> directions;
    List<Double> values;


    public Checks() {
        this.featureIndices = new ArrayList<>();
        this.featureNames = new ArrayList<>();
        this.thresholds = new ArrayList<>();
        this.directions = new ArrayList<>();
        this.values = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<featureIndices.size();i++){
            int featureIndex = featureIndices.get(i);
            String featureName = featureNames.get(i);
            double threshold = thresholds.get(i);
            double featureValue = values.get(i);
            boolean direction = directions.get(i);
            sb.append("feature ").append(featureIndex)
                    .append("(").append(featureName).append(")").append(" ");
            if (direction){
                sb.append(featureValue).append("<=").append(threshold).append(", ");
            } else {
                sb.append(featureValue).append(">").append(threshold).append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Checks that = (Checks) o;

        if (!directions.equals(that.directions)) return false;
        if (!featureIndices.equals(that.featureIndices)) return false;
        if (!featureNames.equals(that.featureNames)) return false;
        if (!thresholds.equals(that.thresholds)) return false;
        if (!values.equals(that.values)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = featureIndices.hashCode();
        result = 31 * result + featureNames.hashCode();
        result = 31 * result + thresholds.hashCode();
        result = 31 * result + directions.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }

    public Checks copy(){
        Checks copy = new Checks();
        for (int i=0;i<this.featureIndices.size();i++){
            copy.featureIndices.add(this.featureIndices.get(i));
            copy.featureNames.add(this.featureNames.get(i));
            copy.thresholds.add(this.thresholds.get(i));
            copy.directions.add(this.directions.get(i));
            copy.values.add(this.values.get(i));
        }
        return copy;
    }

    public static class Serializer extends JsonSerializer<Checks>{
        @Override
        public void serialize(Checks checks, JsonGenerator jsonGenerator,
                              SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
//            jsonGenerator.writeStartObject();
//            jsonGenerator.writeFieldName("checks");
            jsonGenerator.writeStartArray();
            for (int i=0;i< checks.featureIndices.size();i++){

                int featureIndex = checks.featureIndices.get(i);
                String featureName = checks.featureNames.get(i);
                double threshold = checks.thresholds.get(i);
                double featureValue = checks.values.get(i);
                boolean direction = checks.directions.get(i);
                jsonGenerator.writeStartObject();
                jsonGenerator.writeNumberField("feature index", featureIndex);
                jsonGenerator.writeStringField("feature name", featureName);
                jsonGenerator.writeNumberField("feature value", featureValue);
                jsonGenerator.writeFieldName("relation");
                if (direction){
                    jsonGenerator.writeString("<=");
                } else {
                    jsonGenerator.writeString(">");
                }
                jsonGenerator.writeNumberField("threshold",threshold);
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
//            jsonGenerator.writeEndObject();
        }
    }
}