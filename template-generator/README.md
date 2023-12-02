# template-generator

This is a template generator for class files used in InceptusNMS. It will generate the boilerplate used in the class files by using reflection.

All `comment` tags will be set to "TODO".

## Using the Template Generator

The template generator is a command line tool that takes in a single argument, the path to the class file to generate a template for.

```bash
# Generates a Template for net.minecraft.CrashReport Class
./gradlew generateTemplate --args="net.minecraft.CrashReport"
```

The generated file will be located in the `docs` folder.

## Things not Generated

Templates may only generate partial or incorrect information.
The following will not be generated:

- Printing Conventions specified in the [CONTRIBUTING.md](../CONTRIBUTING.md) file
- Deprecation Status
- Generic Information
- Annotations
- `{this}`, `{pkg}` and other type operators

### Fields

- Valid Constant Values that are not primitive types or `string`

### Constructors

- Constructor Operators

### Methods

- Method Operators