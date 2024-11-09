# Data Masker

## Description

Data masker takes databases and masks sensitive information. 
You will produce a command line tool that takes as arguments:

- a path to a json file of data
- a path to a file containing a json array of strings in this format: "k:<regex>" OR "v:<regex>"
    - The first part refers to whether the regex should match the key "k" or the value "v".
        - If the key is used for matching (e.g. `k:^Foo`), replace **values** with strings of the same length but
          consisting of `*` only, where the **key** in the data file matches the regex pattern.
        - If the value is used for matching (e.g. `v:^bar$`), replace the parts of the **values** that match the regex
          pattern with strings of the same length but consisting of `*` only.

The program will be called on the command line like this:
```bash
./gradlew clean fatJar
java -jar build/libs/Data-Maker-1.0-SNAPSHOT-all.jar ./TestData/people.json ./TestData/a.rules.json
```

### Example

`java -jar build/libs/Data-Maker-1.0-SNAPSHOT-all.jar ./TestData/people.json ./TestData/b.rules.json`
produces:

```json
[
  {
    "Name": "****",
    "Email": "****@example.com"
  },
  {
    "Name": "*******",
    "Email": "*******@redgate.com"
  }
]
```
while people.json (data file) is: 
```json
[
  {
    "Name": "Jack",
    "Email": "Jack@example.com"
  },
  {
    "Name": "Bethany",
    "Email": "Bethany@redgate.com"
  }
]
```
and b.rules.json (rule file) is: 
```json
[
  "k:Name",
  "v:^\\w+(?=@\\w+\\.com$)"
]
```