import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public class DataMasker {

  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java DataMasker <data_json_file> <rules_json_file>");
      System.exit(1);
    }

    String dataFilePath = args[0];
    String rulesFilePath = args[1];

    ObjectMapper mapper = new ObjectMapper();

    try {
      JsonNode dataNode = mapper.readTree(new File(dataFilePath));
      List<String> rulesList = Arrays.asList(mapper.readValue(new File(rulesFilePath), String[].class));
      List<MaskingRule> maskingRules = parseRules(rulesList);

      JsonNode maskedData = applyMasking(dataNode, maskingRules);

      System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(maskedData));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static List<MaskingRule> parseRules(List<String> rules) {
    List<MaskingRule> maskingRules = new ArrayList<>();
    for (String rule : rules) {
      if (rule.startsWith("k:") || rule.startsWith("v:")) {
        maskingRules.add(new MaskingRule(rule.substring(0, 2), rule.substring(2)));
      } else {
        System.err.println("Invalid rule format: " + rule);
      }
    }
    return maskingRules;
  }

  private static JsonNode applyMasking(JsonNode node, List<MaskingRule> rules) {
    if (node.isObject()) {
      ObjectNode objectNode = (ObjectNode) node;
      ObjectNode newObjectNode = objectNode.objectNode();
      Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();

      while (fields.hasNext()) {
        Map.Entry<String, JsonNode> entry = fields.next();
        String key = entry.getKey();
        JsonNode value = entry.getValue();

        boolean keyMatched = false;
        for (MaskingRule rule : rules) {
          if (rule.isKeyRule() && rule.matches(key)) {
            keyMatched = true;
            break;
          }
        }

        JsonNode newValue = value;
        if (keyMatched && value.isTextual()) {
          // Mask the entire value
          newValue = new TextNode(maskString(value.asText()));
        } else if (value.isTextual()) {
          // Apply value-masking rules
          String textValue = value.asText();
          String maskedValue = textValue;
          for (MaskingRule rule : rules) {
            if (rule.isValueRule()) {
              maskedValue = maskMatchedParts(maskedValue, rule.getPattern());
            }
          }
          newValue = new TextNode(maskedValue);
        }

        newObjectNode.set(key, newValue);
      }

      return newObjectNode;

    } else if (node.isArray()) {
      ArrayNode arrayNode = (ArrayNode) node;
      ArrayNode newArrayNode = arrayNode.arrayNode();
      for (JsonNode element : arrayNode) {
        newArrayNode.add(applyMasking(element, rules));
      }
      return newArrayNode;

    } else {
      // For this version, we are not processing nested objects or arrays within values
      return node;
    }
  }

  private static String maskString(String input) {
    char[] maskedChars = new char[input.length()];
    Arrays.fill(maskedChars, '*');
    return new String(maskedChars);
  }

  private static String maskMatchedParts(String input, Pattern pattern) {
    Matcher matcher = pattern.matcher(input);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String match = matcher.group();
      String masked = maskString(match);
      matcher.appendReplacement(sb, masked);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static class MaskingRule {
    private final String type;
    private final Pattern pattern;

    public MaskingRule(String type, String regex) {
      this.type = type;
      this.pattern = Pattern.compile(regex);
    }

    public boolean isKeyRule() {
      return "k:".equals(type);
    }

    public boolean isValueRule() {
      return "v:".equals(type);
    }

    public boolean matches(String input) {
      return pattern.matcher(input).matches();
    }

    public Pattern getPattern() {
      return pattern;
    }
  }
}
