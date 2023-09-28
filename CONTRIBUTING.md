# Contributing Guidelines

InceptusNMS is an open source project and we love to receive contributions from our community: you! There are many ways to contribute, from writing tutorials or blog posts, improving the documentation, submitting bug reports and feature requests or writing code which can be incorporated into InceptusNMS itself.

## Creating new Documentation

Simply submit a new PR with your JSON file describing the new or updated documentation for a NMS or CraftBukkit file. We have a list of helpful information listed below.

### Void Methods

When documenting methods that reutrn `void`, please omit the `return` value in the JSON file. This is because the parser will automatically add the `return` value of void for you.

### Type Aliases

In order for the parser to successfully generate JavaDocs, we need to input all return, parameter, and field types, which can get tedious for common types. We have a list of small aliases listed below.

| Alias       | Actual Type                            |
|-------------|----------------------------------------|
| `boolean`   | prmivitive `boolean`                   |
| `byte`      | prmivitive `byte`                      |
| `char`      | prmivitive `char`                      |
| `double`    | prmivitive `double`                    |
| `float`     | prmivitive `float`                     |
| `int`       | prmivitive `int`                       |
| `long`      | prmivitive `long`                      |
| `short`     | prmivitive `short`                     |
| `string`    | `java.lang.String`                     |
| `date`      | `java.util.Date`                       |
| `wboolean`  | `java.lang.Boolean`                    |
| `wbyte`     | `java.lang.Byte`                       |
| `wchar`     | `java.lang.Character`                  |
| `wdouble`   | `java.lang.Double`                     |
| `wfloat`    | `java.lang.Float`                      |
| `wint`      | `java.lang.Integer`                    |
| `wlong`     | `java.lang.Long`                       |
| `wshort`    | `java.lang.Short`                      |
| `file`      | `java.io.File`                         |
| `component` | `net.minecraft.network.chat.Component` |

#### Generics

For Generic Types, simply include the type in the alias. For example:

- `java.util.List<string>`
- `java.util.Map<string, wint>`
- `java.util.List<java.util.Map<file, wlong>>`
- `java.util.Set<org.bukkit.entity.Player>`

### Documentation Conventions

We document all fields and methods, regardless of visibility. This includes package-protected and private methods and fields. The only (current) exception are constructors on enum classes.

#### `class` object

- This object should always be the first object in the JSON file.
- `type` should always be the first line in the object.
- `enclosing`, if provided, should be after `type`. This is only necessary for subclasses.
- `visibility`, if provided, should be after `enclosing` or `type`. If not provided, it is implied to be `public`.
- `mods`, if provided, should be after `enclosing`, `visibility` or `type`.
  - This array must be in the order that the modifiers are declared. For example: `public static final` should be `["static", "final"]`.  
- `comment` should always be the last line in the object.

#### `enumerations` array

- This array should always be the second object in the JSON file if `class` specifies that the type is an enum.
- The objects in this array should be in the order of the enum constants.
- For each individual object:
  - `name` should be the name of the enum constant, case sensitive, and should always be the first line in the object.
  - `comment` should always be the last line in the object.

#### `fields` object
- This object should always be the third object (for enums) or second object (for non-enums) in the JSON file.
- The objects in this object should have their keys be the field name as case sensitive, and must be in the order declared on the class file.
  - `type` should always be the first line in the object.
  - `visibility`, if provided, should be after `type`. If not provided, it is implied to be `public`.
  - `mods`, if provided, should be after `visibility` or `type`.
    - This array must be in the order that the modifiers are declared. For example: `public static final` should be `["static", "final"]`. 
  - `comment` should always be the last line in the object.

### `constructors` array
- This array should always be the fourth object (for enums) or third object (for non-enums) in the JSON file. If not provided, no constructor will be generated.
- This cannot be provided if `class` indicates an interface.
- The objects in this array should be in the order of the constructors declared on the class file.
  - `visibility`, if provided, should be the first line in the object. If not provided, it is implied to be `public`.
  - `params`, if provided, should be after `visibility`.
    - This array must be in the order that the parameters are declared. For example: `Object(int a, int b)` should be `[{"type": "int", "name": "a"}, {"type": "int", "name": "b"}]`.
  - `comment` should always be the last line in the object.

#### `methods` object
- This object should always be the fifth object (for enums) or fourth object (for non-enums) in the JSON file. If not provided, no methods will be generated.
- The objects in this object should have their keys be the method name as case sensitive, and must be in the order declared on the class file.
  - `visibility`, if provided, should be the first line in the object. If not provided, it is implied to be `public`.
  - `mods`, if provided, should be after `visibility`.
    - This array must be in the order that the modifiers are declared. For example: `public static final` should be `["static", "final"]`. 
  - `params`, if provided, should be after `mods` or `visibility` .
    - This array must be in the order that the parameters are declared. For example: `int a, int b` should be `[{"type": "int", "name": "a"}, {"type": "int", "name": "b"}]`. If a parameter has a generic or undescriptive name (e.g. `arg1`), it can be renamed to a more descriptive one, such as the field name it is being mapped to.
  - `return` should be after `params`, `mods`, or `visibility`. If none are provided, it should be the first line in the object. This is also optional if the method returns `void`.
  - `comment` should always be the last line in the object.

#### Package Info Documentation

Each Package should have a `package-info.json` file, containing a `comment` attribute. This comment should be the first and only line in the file.

#### Subclasses Documentation

For generating JavaDocs for Subclasses, simply provide a Dollar Sign (`$`) for each layer. For Example:

- `Class$Parent$SubclassDocumenting.json`

Additionally, an `enclosing` attribute must be provided before `visibility` in the `class` object.