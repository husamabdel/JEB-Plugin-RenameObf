import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pnfsoftware.jeb.core.IEnginesContext;
import com.pnfsoftware.jeb.core.units.IBinaryUnit;
import com.pnfsoftware.jeb.core.units.code.IClass;

public class Deobfuscator {
    
    // Define a regular expression pattern to match class names in log strings
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("(\\w+\\.)*\\w+");

    public static void main(String[] args) {
        // Obtain the JEB engines context
        IEnginesContext enginesContext = new EngCfgBuilder().setEnginesHome("jeb-engines").build();
        
        // Obtain the binary unit that contains the class
        IBinaryUnit binaryUnit = enginesContext.getUnitFactory().getUnit(args[0]);
        
        // Obtain the decompiled class
        IClass decompiledClass = binaryUnit.getDecompiledClass();
        String decompiledCode = decompiledClass.getSource();
        
        // Create a mapping to store the obfuscated class names and their corresponding identified class names
        HashMap<String, String> classMapping = new HashMap<String, String>();
        
        // Parse the decompiled code to identify all hard-coded log strings
        Matcher matcher = Pattern.compile("LOG\\.(\\w+)\\(.*\"(.*)\".*\\)").matcher(decompiledCode);
        
        // Extract the class names used in the log strings and create a mapping between the obfuscated and identified class names
        while (matcher.find()) {
            String logLevel = matcher.group(1);
            String logMessage = matcher.group(2);
            
            Matcher classNameMatcher = CLASS_NAME_PATTERN.matcher(logMessage);
            if (classNameMatcher.find()) {
                String identifiedClassName = classNameMatcher.group();
                String obfuscatedClassName = findObfuscatedClassName(decompiledCode, identifiedClassName);
                classMapping.put(obfuscatedClassName, identifiedClassName);
            }
        }
        
        // Use the mapping to rename the obfuscated class names in the decompiled code
        for (String obfuscatedClassName : classMapping.keySet()) {
            decompiledCode = decompiledCode.replaceAll(obfuscatedClassName, classMapping.get(obfuscatedClassName));
        }
        
        // Use the JEB API to update the decompiled code with the renamed class names
        decompiledClass.updateContent(decompiledCode);
    }
    
    // This method should search the decompiled code for the obfuscated class name and return the identified class name
    private static String findObfuscatedClassName(String decompiledCode, String identifiedClassName) {
        // Use regular expression matching to identify the obfuscated class name
        String regex = "((?:\\w+\\.)*)" + Pattern.quote(identifiedClassName);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(decompiledCode);
        if (matcher.find()) {
            return matcher.group().replace(".", "/");
        } else {
            return null;
        }
    }
}
