import java.io.*;
import java.util.*;
import java.io.FileReader;
import java.util.Iterator;
 
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
 
 
public class Driver_K_Means
{
 
    
    public static void main(String[] args) 
    {
        JSONArray trainRecipeArray = parseJSON("/Users/jh5cj/Desktop/K_Means_Cooking_NEW/train.txt");
        JSONArray testRecipeArray = parseJSON("/Users/jh5cj/Desktop/K_Means_Cooking_NEW/test.txt");

        LinkedList<String> ingredientList = makeIngredientList(trainRecipeArray);
       
        Object[] ingredientArray = ingredientList.toArray();
        Arrays.sort(ingredientArray);

        int[] totalIngredientCountArray = countAllIngredients(trainRecipeArray, ingredientArray);

        //printIngredients(ingredientArray);
        //printIngredientsAndTotalCounts(ingredientArray, totalIngredientCountArray);

        Object[] cuisineArray = extractCuisines(trainRecipeArray);


        combineIdAndAnswer(testRecipeArray, cuisineArray);
        System.exit(0);
        convert_JSON_to_CSV(testRecipeArray, ingredientArray, cuisineArray);



        



        //printCuisines(cuisineArray);

        int[][] cuisineSpecificIngredientCountArrays = countAllIngredientsForCuisines(trainRecipeArray, 
            ingredientArray, cuisineArray);

        printIngredientsAndTotalCuisineCounts(ingredientArray, cuisineSpecificIngredientCountArrays, cuisineArray);

        double[][] ingredientWeightByCuisine = weightIngredients(totalIngredientCountArray, 
            cuisineSpecificIngredientCountArrays, cuisineArray);

        //printIngredientsAndCuisineWeights(ingredientArray, ingredientWeightByCuisine, cuisineArray);

        int numberOfIngredientsToPrint = 15;
        //printSortedWeightsForAllCuisines(numberOfIngredientsToPrint, ingredientArray, cuisineArray, ingredientWeightByCuisine);
        

        //printSubmission(trainRecipeArray, cuisineArray, ingredientWeightByCuisine, ingredientArray);
        printSubmission(testRecipeArray, cuisineArray, ingredientWeightByCuisine, ingredientArray);

        //printPercentCorrect(trainRecipeArray, cuisineArray, ingredientWeightByCuisine, ingredientArray);
       
    }


    public static void combineIdAndAnswer(JSONArray recipeArray, Object[] cuisineArray)
    {
        try
        {
            FileWriter fw = new FileWriter(new File("finalOutput.txt"));
            Scanner sc = new Scanner(new File("testOutput.txt"));

            fw.append("id,cuisine\n");


            // For each recipe
            for(int i = 0; i < recipeArray.size(); i++)
            {
                System.out.println("recipe#" + i);
                
                Object id = (Object) ((JSONObject) recipeArray.get(i)).get("id");

                if(id != null && sc.hasNextInt())
                {
                    int index = sc.nextInt() - 1;
                    String cuisine = (String) cuisineArray[index];

                    fw.append(id + "," + cuisine +"\n");
                }
            }


            fw.close();
            sc.close();
        }
        catch(Exception e)
        {
            System.err.println("error");
        }
    }


    public static void convert_JSON_to_CSV(JSONArray recipeArray, Object[] ingredientArray, Object[] cuisineArray)
    {
        try
        {
            FileWriter fw = new FileWriter(new File("test"));

            // For each recipe
            for(int i = 0; i < recipeArray.size(); i++)
            {

                // For each possible ingredient
                for(int j = 0; j < ingredientArray.length; j++)
                {
                    int bit = 0;

                    JSONArray ingredients = (JSONArray) ((JSONObject) recipeArray.get(i)).get("ingredients");

                    // For each actual ingredient
                    for(int k = 0; k < ingredients.size(); k++)
                    {
                        Object ingr = ingredients.get(k);
                        String s = (String) ingr;
                        

                        if(s.equals(ingredientArray[j]))
                        {
                            bit = 1;
                        }
                    }

                    fw.append(bit + " ");
                }
                
                System.out.println(i+1);

                /*
                Object cuisineObj = ((JSONObject) recipeArray.get(i)).get("cuisine");
                String cuisineStr = (String) cuisineObj;

                int cuisineIndex = Arrays.binarySearch(cuisineArray, cuisineStr);

                // Append cuisine index
                fw.append(cuisineIndex + "\n");
                */
                fw.append("\n");
            }
            System.out.println();


            fw.close();
        }
        catch(Exception e)
        {

        }
    }


    public static void printPercentCorrect(JSONArray recipeArray, Object[] cuisineArray, double[][] ingredientWeightByCuisine, 
        Object[] ingredientArray)
    {
        int numCorrect = 0;
        int totalNum = 0;

        for(int i = 0; i < recipeArray.size(); i++)
        {
            String prediction = predictRecipe(recipeArray, i, cuisineArray, ingredientWeightByCuisine, ingredientArray);
            Object cuisineObj = ((JSONObject) recipeArray.get(i)).get("cuisine");
            if(prediction.equals((String) cuisineObj))
            {
                numCorrect++;
            }
            totalNum++;
        }

        System.out.println(numCorrect + " correct out of " + totalNum + ": " + ((double) numCorrect / totalNum));
    }


    public static void printSortedWeightsForAllCuisines(int numberOfIngredientsToPrint, Object[] ingredientArray, 
        Object[] cuisineArray, double[][] ingredientWeightByCuisine)
    {
        for(int i = 0; i < cuisineArray.length; i++)
        {
            int cuisineIndex = i;

            printSortedWeightsForSpecificCuisine(cuisineIndex, numberOfIngredientsToPrint, ingredientArray, 
                cuisineArray, ingredientWeightByCuisine);
        }
    }


    public static void printSubmission(JSONArray recipeArray, Object[] cuisineArray, double[][] ingredientWeightByCuisine,
        Object[] ingredientArray)
    {
        System.out.println("id,cuisine");


        try
        {
            FileWriter fw = new FileWriter(new File("output"));

    

            fw.append("id,cuisine\n");

            for(int i = 0; i < recipeArray.size(); i++)
            {
                int recipeIndex = i;
                String prediction = predictRecipe(recipeArray, recipeIndex, cuisineArray, ingredientWeightByCuisine, ingredientArray);
                //System.out.println(recipeIndex + ": " + prediction); // DEBUG
                System.out.println(prediction);

                fw.append(prediction + "\n");
                
                // DEBUG===========================================================================
                //if(i == 10) System.exit(0);
                // END DEBUG=======================================================================
        
            }

            fw.close();
        }
        catch(Exception e)
        {

        }


    }


    public static String predictRecipe(JSONArray recipeArray, int recipeIndex, Object[] cuisineArray, 
        double[][] ingredientWeightByCuisine, Object[] ingredientArray)
    {
        Object idObj = ((JSONObject) recipeArray.get(recipeIndex)).get("id");
        long idLong = (long) idObj;
        int idInt = (int) idLong;
        String id = idInt + "";


        String predictedCuisine = "no_cuisine";

        double[] cuisineWeights = new double[cuisineArray.length];

        JSONArray ingredients = (JSONArray) ((JSONObject) recipeArray.get(recipeIndex)).get("ingredients");


        for(int i = 0; i < ingredients.size(); i++)
        {

            //String currentIngredient = (String) ingredients.get(i);

            int ingredientIndex = Arrays.binarySearch(ingredientArray, ingredients.get(i));
            //System.out.println(ingredientArray[ingredientIndex]);

            // Only add weights for ingredients seen in training
            if(ingredientIndex >= 0)
            {
                for(int j = 0; j < cuisineArray.length; j++)
                {
                    cuisineWeights[j] += ingredientWeightByCuisine[j][ingredientIndex];
                }
            }
        }


        int predictedCuisineIndex = -1;
        double predictedCuisineWeight = -1.0;
        for(int i = 0; i < cuisineArray.length; i++)
        {
            if(cuisineWeights[i] > predictedCuisineWeight)
            {
                predictedCuisineWeight = cuisineWeights[i];
                predictedCuisineIndex = i;
                predictedCuisine = (String) cuisineArray[i];
            }
            //System.out.println(cuisineWeights[i]);
        }
        //System.out.println();


        return id + "," + predictedCuisine;
        //return predictedCuisine;
    }


    // 0 <= cuisineIndex <= 19
    public static void printSortedWeightsForSpecificCuisine(int cuisineIndex, int numIngredientsToPrint, 
        Object[] ingredientArray, Object[] cuisineArray, double[][] ingredientWeightByCuisine)
    {
        System.out.println(); 
        System.out.println("Cuisine: " + cuisineArray[cuisineIndex]);
        System.out.println("++++++++++++++++++++++++++++++++++++"); 

        LinkedList<Integer> indexToLargestWeights = new LinkedList<Integer>();

        // PRINT TOP X INGREDIENTS
        for(int i = 0; i < numIngredientsToPrint; i++)
        {
            int indexOfLargest = -1;
            double largestSoFar = -1.0;

            // FOR EACH POSSIBLE INGREDIENT
            for(int j = 0; j < ingredientArray.length; j++)
            {
                double current = ingredientWeightByCuisine[cuisineIndex][j];

                // IF ITS WEIGHT FOR THIS CUISINE IS THE LARGEST SO FAR AND NOT ALREADY IN TOP X LIST, ADD IT
                if(  (current > largestSoFar)  &&  (indexToLargestWeights.contains(j) == false)  )
                {
                    indexOfLargest = j;
                    largestSoFar = current;
                }
            }

            // YOU HAVE A NEW INGREDIENT NOT IN TOP X LIST THAT CAN BE ADDED
            if(indexToLargestWeights.contains(indexOfLargest) == false)
            {
                indexToLargestWeights.add(indexOfLargest);
            }
        }

        double[] largestWeights = new double[numIngredientsToPrint];

        // PRINT THE TOP X INGREDIENTS
        for(int i = 0; i < numIngredientsToPrint; i++)
        {
            int ingredientIndex = indexToLargestWeights.get(i);

            largestWeights[i] = ingredientWeightByCuisine[cuisineIndex][ingredientIndex];

            System.out.println(ingredientIndex + ": " + ingredientArray[ingredientIndex] + " " + largestWeights[i]);
        }
        System.out.println(); 
    }


    public static void printIngredientsAndCuisineWeights(Object[] ingredientArray, 
        double[][] ingredientWeightByCuisine, Object[] cuisineArray)
    {
        for(int i = 0; i < ingredientArray.length; i++)
        {
            System.out.print(ingredientArray[i] + " ");

            for(int j = 0; j < cuisineArray.length; j++)
            {
                System.out.printf("%.2f  ", ingredientWeightByCuisine[j][i]);
            }

            System.out.println();
        }
    }


    public static double[][] weightIngredients(int[] totalIngredientCountArray, 
        int[][] cuisineSpecificIngredientCountArrays, Object[] cuisineArray)
    {
        double[][] ingredientWeightByCuisine = new double[cuisineArray.length][totalIngredientCountArray.length];

        for(int i = 0; i < totalIngredientCountArray.length; i++)
        {
            for(int j = 0; j < cuisineArray.length; j++)
            {
                ingredientWeightByCuisine[j][i] = (double) cuisineSpecificIngredientCountArrays[j][i] / totalIngredientCountArray[i];
            }
        }

        return ingredientWeightByCuisine;
    }


    public static void printIngredientsAndTotalCuisineCounts(Object[] ingredientArray, 
        int[][] cuisineSpecificIngredientCountArrays, Object[] cuisineArray)
    {
        for(int i = 0; i < ingredientArray.length; i++)
        {
            System.out.print(ingredientArray[i] + " ");

            for(int j = 0; j < cuisineArray.length; j++)
            {
                System.out.print(cuisineSpecificIngredientCountArrays[j][i] + " ");
            }

            System.out.println();
        }
    }


    public static int[][] countAllIngredientsForCuisines(JSONArray recipeArray, Object[] ingredientArray, 
        Object[] cuisineArray)
    {
        
        int[][] cuisineSpecificIngredientCountArrays = new int[cuisineArray.length][ingredientArray.length];

        for(int i = 0; i < recipeArray.size(); i++)
        {
            JSONArray ingredients = (JSONArray) ((JSONObject) recipeArray.get(i)).get("ingredients");

            Object cuisineObj = ((JSONObject) recipeArray.get(i)).get("cuisine");
            String thisCuisine = (String) cuisineObj;
            int cuisineIndex = Arrays.binarySearch(cuisineArray, thisCuisine);

            //System.out.println("recipe " + (i+1));
            //System.out.println("cuisine " + thisCuisine + " " + cuisineIndex);

            for(int j = 0; j < ingredients.size(); j++)
            {
                int ingredientIndex = Arrays.binarySearch(ingredientArray, ingredients.get(j));

                cuisineSpecificIngredientCountArrays[cuisineIndex][ingredientIndex]++;

                //System.out.println(ingredients.get(j));
            }

            //System.exit(0);
        }

        return cuisineSpecificIngredientCountArrays;
    }


    public static void printCuisines(Object[] cuisineArray)
    {
        System.out.println(cuisineArray.length);

        for(int i = 0; i < cuisineArray.length; i++)
        {
            System.out.println(cuisineArray[i]);
        }
    }


    public static Object[] extractCuisines(JSONArray recipeArray)
    {
        LinkedList<String> cuisineList = new LinkedList<String>();

        for(int i = 0; i < recipeArray.size(); i++)
        {
            String thisCuisine = (String) ((JSONObject) recipeArray.get(i)).get("cuisine");

            if(cuisineList.contains(thisCuisine) == false)
            {
                cuisineList.add(thisCuisine);
            }
        }

        Object[] cuisineArray = cuisineList.toArray();
        Arrays.sort(cuisineArray);

        return cuisineArray;
    }


    public static void printIngredientsAndTotalCounts(Object[] ingredientArray, int[] totalIngredientCountArray)
    {
        for(int i = 0; i < totalIngredientCountArray.length; i++)
        {
            System.out.println(ingredientArray[i] + " " + totalIngredientCountArray[i]);
        }
    }


    public static void printIngredients(Object[] ingredientArray)
    {
        for(int i = 0; i < ingredientArray.length; i++)
        {
            System.out.println(ingredientArray[i]);
        }
    }


    public static LinkedList<String> makeIngredientList(JSONArray recipeArray)
    {
        LinkedList<String> ingredientList = new LinkedList<String>();

        for(int i = 0; i < recipeArray.size(); i++)
        {
            JSONArray ingredients = (JSONArray) ((JSONObject) recipeArray.get(i)).get("ingredients");

            for(int j = 0; j < ingredients.size(); j++)
            {
                if(ingredientList.contains(ingredients.get(j)) == false) 
                {
                    ingredientList.add((String) ingredients.get(j));
                }
            }
        }

        return ingredientList;
    }


    public static int[] countAllIngredients(JSONArray recipeArray, Object[] ingredientArray)
    {
        
        int[] totalIngredientCountArray = new int[ingredientArray.length];

        for(int i = 0; i < recipeArray.size(); i++)
        {
            JSONArray ingredients = (JSONArray) ((JSONObject) recipeArray.get(i)).get("ingredients");

            for(int j = 0; j < ingredients.size(); j++)
            {
                int index = Arrays.binarySearch(ingredientArray, ingredients.get(j));
                totalIngredientCountArray[index]++;
            }
        }

        return totalIngredientCountArray;
    }


    @SuppressWarnings("unchecked")
    public static JSONArray parseJSON(String path)
    {
        JSONParser parser = new JSONParser();
 
        try 
        {
 
            Object obj = parser.parse(new FileReader(path));
            JSONArray superArray = (JSONArray) obj;

            return superArray;
 
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}