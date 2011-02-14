
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;

/**
 * @version $Rev$ $Date$
 */
public class Sections {
    public static void main(String[] args) throws Exception {
        if (args.length != 2){
            System.err.println("args: <current> <sectionsFile>");
        }

        complete(args[0], args[1]);
    }

    public static void complete(String cur, String sectionsFile) throws IOException {
        List<String> options = new ArrayList();
        String[] parts = cur.split("\\.");

        File file = new File(sectionsFile);

        FileReader fileReader = new FileReader(file);
        BufferedReader in = new BufferedReader(fileReader);

        String section = in.readLine();

        boolean moreOptions = false;

        while (section != null){
            try {
                if (!section.startsWith(cur)) continue;

                String[] p = section.split("\\.");

                int pos = parts.length + 1;

                if (!cur.endsWith(".")){
                    pos--;
                }

                String packge = "";
                for (int i = 0; i < p.length && i < pos; i++) {
                    String s = p[i];
                    packge += s;
                    if (!section.equals(packge)){
                        packge += ".";
                    }
                }

                if (!section.equals(packge)){
                    moreOptions = true;
                }

                if (!options.contains(packge)) options.add(packge);
            } finally {
                section = in.readLine();
            }
        }

        if (moreOptions && options.size() > 0){
            options.add(options.get(options.size()-1)+"...");
        }
        for (String s : options) {
            System.out.println(s);
        }
    }

}
