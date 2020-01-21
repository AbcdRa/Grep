import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Grep {
    private static String filename = "";
    private static ArrayList<String> keyWords = new ArrayList<>();
    private static boolean ignoreCase = false;
    private static boolean isRegularExpression = false;
    private static boolean isMoreKeyWords = false;
    private static boolean isSplitText = true;
    private static ArrayList<String> text;

    private final static int BUFFER_SIZE = 1000;

    //Вернет true если распознать аргументы удалось, false - если аргументов нет
    private static boolean parseArgs(String[] args) {
        //Если аргументов нет пишем справку пользователю
        if(args.length == 0) {
            System.out.println("Haven't arguments.\nSyntax: filename [keyWord1] ... [keyWordN] [[-i], [-r], [-s], [-u] ");
            return false;
        }
        //Если аргумент один, то это должно быть имя файла
        else if(args.length == 1) {
            filename = args[0];
            System.out.println("File: " + filename);
        }
        //Если аргумента два, это точно должно быть имя файла и слово поиска
        else if(args.length == 2) {
            filename = args[0];
            keyWords.add(args[1]);
            System.out.println("File: " + filename);
            System.out.println("KeyWord: " + keyWords.get(0));
        }
        //В других случаях
        else {
            for (String arg : args) {
                if (arg.equals("-i") && !ignoreCase) {
                    ignoreCase = true;
                } else if (arg.equals("-r") && !isRegularExpression) {
                    isRegularExpression = true;
                } else if (arg.equals("-s") && !isMoreKeyWords) {
                    isMoreKeyWords = true;
                } else if (filename.equals("")) {
                    filename = arg;
                } else if (arg.equals("-u") && isSplitText) {
                        isSplitText = false;
                } else {
                    keyWords.add(arg);
                }
            }
        }
        return true;
    }

    private static String joinLineInText() {
        StringBuilder out = new StringBuilder();
        for(String line : text) {
            out.append(line);
        }
        return out.toString();
    }

    //Получаем массив строк из файла, по пути filename
    private static ArrayList<String> getTextFromFile(String filename) {
        ArrayList<String> text = new ArrayList<>();
        try (FileReader fileReader = new FileReader(filename)){
            InputStreamReader in = new InputStreamReader(new FileInputStream(filename), Charset.defaultCharset());
            StringBuilder line = new StringBuilder();
            int currCode = in.read();
            while (currCode != -1) {
                line.append((char) currCode);
                if((char) currCode == '\n') {
                    text.add(line.toString());
                    line = new StringBuilder();
                }
                currCode = in.read();
            }
            text.add(line.toString()+'\n');
        } catch (FileNotFoundException e) {
            System.out.println("File not found :(");
            return null;
        } catch (IOException e) {
            System.out.println("Input/Output Error: " + e.getMessage());
            return null;
        }
        return  text;
    }

    //Для поддержки кириллицы, декодируем текст из utf-8 в cp1251
    private static ArrayList<String> encodeText(String filename) {
        String encodeFileName = filename.substring(0, filename.length() - 4) + "cp1251.txt";
        try (final FileInputStream    fileInputStream    = new FileInputStream(filename);
             final InputStreamReader  inputStreamReader  = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
             final FileOutputStream   fileOutputStream   = new FileOutputStream(encodeFileName);
             final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "windows-1251"))
        {
            final char[] buffer = new char[BUFFER_SIZE];
            int readed;
            while ((readed = inputStreamReader.read(buffer, 0, BUFFER_SIZE)) > 0)
                outputStreamWriter.write(buffer, 0, readed);
        } catch (IOException e) {
            System.out.println("Error in decoding file");
            e.printStackTrace();
        }
        Grep.filename = encodeFileName;
        return getTextFromFile(filename);
    }

    //Самый простой метод поиска одно слово, игнор регистра
    private static void baseSearch(String keyWord, ArrayList<String> text) {
        int countMatch = 0;
        if(isSplitText) {
            for (String line : text) {
                int countMatchOnLine = baseLineSearch(keyWord, line, text.indexOf(line));
                countMatch += countMatchOnLine;
                if (countMatchOnLine > 0)
                    System.out.println("Matches found in string: " + countMatchOnLine);
            }
        } else {
            countMatch = baseLineSearch(keyWord, joinLineInText(), 0);
        }
        System.out.println("Matches found for text: " + countMatch);
    }

    //Поиск слова по строке
    private static int baseLineSearch(String keyWord, String line, int index) {
        int countMatch = 0;
        if(ignoreCase) {
            int position = line.toUpperCase().indexOf(keyWord.toUpperCase());
            while (position != -1) {
                countMatch++;
                displayMatch(keyWord, line, index, position);
                line = line.substring(position + 1);
                position = line.indexOf(keyWord);
            }
        }
        else {
            int position = line.indexOf(keyWord);
            while (position != -1) {
                countMatch++;
                displayMatch(keyWord, line, index, position);
                line = line.substring(position + 1);
                position = line.indexOf(keyWord);
            }
        }
        return countMatch;
    }

    //Вывод в консоль найденного совпадения
    private static void displayMatch(String keyWord, String line, int index, int position) {
        if(line.length() < 140) {
            System.out.println((index + 1) + ") " + keyWord + " : " + line);
        }
        else {
            line = line.substring(position > 65 ? position - 65 : 0,
                    (line.length() - position) > 65 ? position + 65 : line.length() - 1);
            System.out.println((index + 1) + ") " + keyWord + " : " + line);
        }
    }

    //Метод поиска по регулярным выражениям
    private static void regularSearch(String pattern, ArrayList<String> text) {
        Pattern pat;
        Matcher mat;
        if(ignoreCase)  pat = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        else pat = Pattern.compile(pattern);
        int countMatch = 0;
        if(isSplitText) {
            for (String line : text) {
                mat = pat.matcher(line);
                int countMatchOnLine = 0;
                while (mat.find()) {
                    countMatchOnLine++;
                    displayMatch(mat.group(), line, text.indexOf(line), mat.start());
                }
                if (countMatchOnLine > 0)
                    System.out.println("Matches found in string: " + countMatchOnLine);
            }
        } else {
            String lineText = joinLineInText();
            mat = pat.matcher(lineText);
            while (mat.find()) {
                countMatch++;
                displayMatch(mat.group(), lineText, 0, mat.start());
            }
        }
        System.out.println("Matches found for text: " + countMatch);
    }

    //Метод, использующий настройки переменных, для подбора необходимого метода поиска
    private static void search() {
        if(isMoreKeyWords) {
            if(isRegularExpression) {
                for(String keyWord : keyWords)
                    regularSearch(keyWord, text);
            }
            else {
                for(String keyWord : keyWords)
                    baseSearch(keyWord, text);
            }
        }
        else {
            if(isRegularExpression) {
                regularSearch(joinKeyWords(), text);
            }
            else {
                if(keyWords.size() == 0) {
                    text.forEach(System.out::print);
                }
                else {
                    baseSearch(joinKeyWords(), text);
                }
            }
        }
    }

    //Метод склейвающий все аргументы в одну строку
    private static String joinKeyWords() {
        StringBuilder out = new StringBuilder();
        for (String keyWord : keyWords) {
            out.append(keyWord).append(" ");
        }
        return out.toString().substring(0,out.length()-1);
    }

    //Точка входа
    public static void main(String[] args) {
        //Парсим аргументы, если возникнет ошибка, то закрываем программу
        if(!parseArgs(args)) {
            System.out.println(" Shutting down ...");
            return;
        }
        text = encodeText(filename);
        search();
    }
}
