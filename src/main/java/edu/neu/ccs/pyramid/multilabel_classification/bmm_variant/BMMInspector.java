package edu.neu.ccs.pyramid.multilabel_classification.bmm_variant;

import edu.neu.ccs.pyramid.classification.Classifier;
import edu.neu.ccs.pyramid.classification.logistic_regression.LogisticRegression;
import edu.neu.ccs.pyramid.classification.logistic_regression.Weights;
import edu.neu.ccs.pyramid.dataset.MultiLabel;
import edu.neu.ccs.pyramid.dataset.MultiLabelClfDataSet;
import edu.neu.ccs.pyramid.eval.Entropy;
import edu.neu.ccs.pyramid.util.ArgSort;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created by chengli on 12/25/15.
 */
public class BMMInspector {
    public static Weights getMean(BMMClassifier bmm, int label){
        int numClusters = bmm.getNumClusters();
        int length = ((LogisticRegression)bmm.getBinaryClassifiers()[0][0]).getWeights().getAllWeights().size();
        int numFeatures = ((LogisticRegression)bmm.getBinaryClassifiers()[0][0]).getNumFeatures();
        Vector mean = new DenseVector(length);
        for (int k=0;k<numClusters;k++){
            mean = mean.plus(((LogisticRegression)bmm.getBinaryClassifiers()[k][label]).getWeights().getAllWeights());
        }

        mean = mean.divide(numClusters);
        return new Weights(2,numFeatures,mean);
    }


    public static double distanceFromMean(BMMClassifier bmm){
        int numClasses = bmm.getNumClasses();
        return IntStream.range(0,numClasses).mapToDouble(l -> distanceFromMean(bmm,l)).average().getAsDouble();
    }

    public static double distanceFromMean(BMMClassifier bmm, int label){
        Classifier.ProbabilityEstimator[][] logistics = bmm.getBinaryClassifiers();
        int numClusters = bmm.getNumClusters();
        int numFeatures =  ((LogisticRegression)logistics[0][0]).getNumFeatures();

        Vector positiveAverageVector = new DenseVector(numFeatures);
        for (int k=0;k<numClusters;k++){
            Vector positiveVector = ((LogisticRegression) logistics[k][label]).getWeights().getWeightsWithoutBiasForClass(1);
            positiveAverageVector = positiveAverageVector.plus(positiveVector);
        }


        positiveAverageVector = positiveAverageVector.divide(numClusters);

        double dis = 0;
        for (int k=0;k<numClusters;k++){

            Vector positiveVector = ((LogisticRegression) logistics[k][label]).getWeights().getWeightsWithoutBiasForClass(1);
            dis += positiveVector.minus(positiveAverageVector).norm(2);
        }
        return dis/numClusters;
    }

    public static List<Map<MultiLabel,Double>> visualizeClusters(BMMClassifier bmm, MultiLabelClfDataSet dataSet){
        int numClusters = bmm.getNumClusters();
        List<Map<MultiLabel,Double>> list = new ArrayList<>();
        for (int k=0;k<numClusters;k++){
            list.add(new HashMap<>());
        }

        for (int i=0;i<dataSet.getNumDataPoints();i++){
            double[] clusterProbs = bmm.getMultiClassClassifier().predictClassProbs(dataSet.getRow(i));
            MultiLabel multiLabel = dataSet.getMultiLabels()[i];
            for (int k=0;k<numClusters;k++){
                Map<MultiLabel,Double> map = list.get(k);
                double count = map.getOrDefault(multiLabel,0.0);
                double newcount = count+clusterProbs[k];
                map.put(multiLabel,newcount);
            }
        }
        return list;
    }



    public static void visualizePrediction(BMMClassifier bmmClassifier, Vector vector){
        int numClusters = bmmClassifier.getNumClusters();
        int numClasses = bmmClassifier.getNumClasses();
        double[] proportions = bmmClassifier.getMultiClassClassifier().predictClassProbs(vector);
        double[][] probabilities = new double[numClusters][numClasses];
        for (int k=0;k<numClusters;k++){
            for (int l=0;l<numClasses;l++){
                probabilities[k][l]=bmmClassifier.getBinaryClassifiers()[k][l].predictClassProb(vector,1);
            }
        }

        int[] sorted = ArgSort.argSortDescending(proportions);
        double[] topProbs = probabilities[sorted[0]];
        MultiLabel trivalPred = new MultiLabel();
        for (int l=0;l<numClasses;l++){
            if (topProbs[l]>=0.5){
                trivalPred.addLabel(l);
            }
        }

        MultiLabel secondPred = new MultiLabel();
        for (int l=0;l<numClasses;l++){
            if (probabilities[sorted[1]][l]>=0.5){
                secondPred.addLabel(l);
            }
        }

        MultiLabel predicted = bmmClassifier.predict(vector);
        if (!predicted.equals(trivalPred)){
            System.out.println("interesting case !");
            if (!predicted.equals(secondPred)){
                System.out.println("very interesting case !");
            }
        }

        System.out.println("probability = "+bmmClassifier.predictMaxProb(vector));
        System.out.println("proportion = "+ Arrays.toString(proportions));
        System.out.println("perplexity="+ Math.pow(2,Entropy.entropy2Based(proportions)));
        for (int t=0;t<sorted.length;t++){
            int k = sorted[t];
            System.out.println("rank "+t);
            System.out.println("cluster "+k);
            System.out.println(proportions[k]);
            StringBuilder sb = new StringBuilder();
//            sb.append("[");
//            for (int l=0;l<numClasses;l++){
//                double p = probabilities[k][l];
//                if (p>0.01){
//                    sb.append(l).append(":").append(p).append(", ");
//                }
//
//            }
//            sb.append("]");
//            System.out.println(sb.toString());
            System.out.println(Arrays.toString(probabilities[k]));
        }
    }
}
