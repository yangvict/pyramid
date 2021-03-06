package edu.neu.ccs.pyramid.application;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.image.AngleGenerator;
import com.kennycason.kumo.palette.ColorPalette;
import com.kennycason.kumo.wordstart.CenterWordStart;
import edu.neu.ccs.pyramid.clustering.bm.BM;
import edu.neu.ccs.pyramid.clustering.bm.BMTrainer;
import edu.neu.ccs.pyramid.configuration.Config;
import edu.neu.ccs.pyramid.dataset.*;
import edu.neu.ccs.pyramid.util.ArgSort;
import edu.neu.ccs.pyramid.util.BernoulliDistribution;
import edu.neu.ccs.pyramid.util.Pair;
import edu.neu.ccs.pyramid.util.Serialization;
import edu.neu.ccs.pyramid.util.*;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cluster labels by Bernoulli Mixtures
 * Created by chengli on 2/26/17.
 */
public class ClusterLabels {
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception{

        if (args.length != 1) {
            throw new IllegalArgumentException("Please specify a properties file.");
        }

        Config config = new Config(args[0]);
        System.out.println(config);

        fitModel(config);
        plot(config);
    }

    private static void fitModel(Config config) throws Exception{
        List<String> labelNames = FileUtils.readLines(new File(config.getString("input.labelNames")));
        List<String> labels = FileUtils.readLines(new File(config.getString("input.labels")));

        int numLabels = labelNames.size();
        int numData = labels.size();

        DataSet dataSet = DataSetBuilder.getBuilder()
                .density(Density.SPARSE_SEQUENTIAL)
                .numDataPoints(numData)
                .numFeatures(numLabels)
                .build();

        for (int i=0;i<numData;i++){
            String line = labels.get(i);
            if (!line.isEmpty()){
                String[] split = line.split(" ");
                for (String s: split){
                    int l = Integer.parseInt(s);
                    dataSet.setFeatureValue(i,l,1);
                }
            }
        }


        System.out.println("data loaded");

        int numClusters = config.getInt("numClusters");

        System.out.println("Start training Bernoulli mixture with EM");
        BMTrainer trainer = new BMTrainer(dataSet,numClusters, 0);
        for (int iter=1;iter<=config.getInt("numIterations");iter++){
            System.out.println("iteration = "+iter);
            trainer.eStep();
            trainer.mStep();
//            if (iter%5==0){
//                System.out.println("obj = "+trainer.getObjective());
//            }
        }
        BM bm = trainer.getBm();


        bm.setNames(labelNames);

        String output = config.getString("output.dir");
        new File(output).mkdirs();
        Serialization.serialize(bm, new File(output, "model"));

        FileUtils.writeStringToFile(new File(output, "model_parameters.txt"), bm.toString());

    }


    public static void plot(Config config) throws Exception{
        BM bm = (BM) Serialization.deserialize(new File(config.getString("output.dir"), "model"));
        double[] coefficients = bm.getMixtureCoefficients();
        int[] sortedComponents = ArgSort.argSortDescending(bm.getMixtureCoefficients());

        File clusterFolder = Paths.get(config.getString("output.dir"),"clusters").toFile();
        clusterFolder.mkdirs();
        FileUtils.cleanDirectory(clusterFolder);

        for (int i=0;i<sortedComponents.length;i++){
            int k = sortedComponents[i];
            List<WordFrequency> frequencies = getCluster(bm, k);

            double max = frequencies.stream().mapToDouble(WordFrequency::getFrequency).max().getAsDouble();
            double sum = frequencies.stream().mapToDouble(WordFrequency::getFrequency).sum();
            double ratio = sum/max;


            final Dimension dimension = new Dimension(600, 600);
            final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.RECTANGLE);
            wordCloud.setPadding(0);
            wordCloud.setAngleGenerator(new AngleGenerator(0));
            wordCloud.setBackground(new RectangleBackground(dimension));

            wordCloud.setColorPalette(buildRandomColorPalette(20));
            wordCloud.setBackgroundColor(Color.WHITE);
            wordCloud.setFontScalar(new LinearFontScalar(20, (int)(500/ratio)));
            wordCloud.setWordStartStrategy(new CenterWordStart());
            wordCloud.build(frequencies);
            File out = Paths.get(config.getString("output.dir"),"clusters",""+i+"_"+coefficients[k]+".png").toFile();
            wordCloud.writeToFile(out.getAbsolutePath());
        }
    }


    private static List<WordFrequency> getCluster(BM bm, int k) throws Exception{

        BernoulliDistribution[][] distributions = bm.getDistributions();
        List<Pair<String,Double>> pairs = new ArrayList<>();
        for (int d=0;d<bm.getDimension();d++){
            Pair<String,Double> pair = new Pair<>(bm.getNames().get(d),distributions[k][d].getP());
            pairs.add(pair);
        }
        Comparator<Pair<String,Double>> comparator = Comparator.comparing(Pair::getSecond);
        List<Pair<String,Double>> sorted = pairs.stream().sorted(comparator.reversed())
                .collect(Collectors.toList());
        List<WordFrequency> frequencies = new ArrayList<>();
        double sum = sorted.stream().filter(pair -> pair.getSecond()>0).limit(20).mapToDouble(Pair::getSecond).sum();
        sorted.stream().filter(pair -> pair.getSecond()>0).limit(20).forEach(pair -> {
            WordFrequency wordFrequency = new WordFrequency(pair.getFirst(), (int)(pair.getSecond()*200/sum));
            frequencies.add(wordFrequency);
        });
        return frequencies;
    }

    private static ColorPalette buildRandomColorPalette(int n) {
        final Color[] colors = new Color[n];
        for(int i = 0; i < colors.length; i++) {
            colors[i] = new Color(RANDOM.nextInt(230) + 25, RANDOM.nextInt(230) + 25, RANDOM.nextInt(230) + 25);
        }
        return new ColorPalette(colors);
    }

}
