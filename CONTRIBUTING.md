# Contributing Guidelines

InceptusNMS is an open source project and we love to receive contributions from our community: you! There are many ways to contribute, from writing tutorials or blog posts, improving the documentation, submitting bug reports and feature requests or writing code which can be incorporated into InceptusNMS itself.

## Creating new Documentation

Simply submit a new PR with your JSON file describing the new or updated documentation for a NMS or CraftBukkit file. We have a list of helpful information listed below.

### Void Methods

When documenting methods that reutrn `void`, please omit the `return` value in the JSON file. This is because the parser will automatically add the `return` value of void for you.

### Type Aliases

In order for the parser to successfully generate JavaDocs, we need to input all return, parameter, and field types, which can get tedious for common types. We have a list of small aliases listed below.

| Alias        | Actual Type                            |
|--------------|----------------------------------------|
| `{this}`     | class representing the JSON file       |
| `obj`        | `java.lang.Object`                     |
| `boolean`    | prmivitive `boolean`                   |
| `byte`       | prmivitive `byte`                      |
| `char`       | prmivitive `char`                      |
| `double`     | prmivitive `double`                    |
| `float`      | prmivitive `float`                     |
| `int`        | prmivitive `int`                       |
| `long`       | prmivitive `long`                      |
| `short`      | prmivitive `short`                     |
| `string`     | `java.lang.String`                     |
| `date`       | `java.util.Date`                       |
| `wboolean`   | `java.lang.Boolean`                    |
| `wbyte`      | `java.lang.Byte`                       |
| `wchar`      | `java.lang.Character`                  |
| `wdouble`    | `java.lang.Double`                     |
| `wfloat`     | `java.lang.Float`                      |
| `wint`       | `java.lang.Integer`                    |
| `wlong`      | `java.lang.Long`                       |
| `wshort`     | `java.lang.Short`                      |
| `file`       | `java.io.File`                         |
| `component`  | `net.minecraft.network.chat.Component` |
| `codec`      | `com.mojang.serialization.Codec`       |
| `deprecated` | `java.lang.Deprecated`                 |
| `bytebuf`    | `io.netty.buffer.ByteBuf`              |
| `nullable`   | `javax.annotation.Nullable`            |

#### Other Aliases

- `{V}` will return the version part for the name of the CraftBukkit package.
  - e.g. `v1_20_R2` 

#### Generics

For Generic Types, simply include the type in the alias. For example:

- `java.util.List<string>`
- `java.util.Map<string,wint>`
- `java.util.List<java.util.Map<file,wlong>>`
- `java.util.Set<org.bukkit.entity.Player>`

There shold be **no spaces** in between generic arguments.

### Documentation Conventions

We document all fields and methods, regardless of visibility. This includes package-protected and private methods and fields. The only (current) exception are constructors on enum classes. Files must also maintain a double-space, CRLF format and must not have tabs. 

#### `class` object

- This object should always be the first object in the JSON file.
- `type` should always be the first line in the object.
- `extends`, if provided, should be after `type`.
- `implements`, if provided, should be after `extends` or `type`.
  - For an interface that extends interfaces, this should be used in place of `extends`.
- `generics`, if provided, should be after `implements`, `extends`, or `type`.
- `enclosing`, if provided, should be after `generics`, `implements`, `extends`, or `type`. This is only necessary for subclasses.
- `visibility`, if provided, should be after `generics`, `enclosing`, `implements`, `extends`, or `type`. If not provided, it is implied to be `public`.
- `mods`, if provided, should be after `generics`, `visibility`, `enclosing`, `implements`, `extends`, or `type`.
  - This array must be in the order that the modifiers are declared. For example: `public static final` should be `["static", "final"]`.  
  - The array must be declared on one line.
- `annotations`, if provided, should be before `comment`.
- `comment` should always be the last line in the object.

#### `enumerations` array

- This array should always be the second object in the JSON file if `class` specifies that the type is an enum.
- The objects in this array should be in the order of the enum constants.
- For each individual object:
  - `name` should be the name of the enum constant, case sensitive, and should always be the first line in the object.
  - `annotations`, if provided, should be after `name`.
  - `comment` should always be the last line in the object.

#### `fields` object

Record Classes will automatically generate fields for you according to the constructor with the most parameters.

- This object should always be the third object (for enums) or second object (for non-enums) in the JSON file.
- The objects in this object should have their keys be the field name as case sensitive, and must be in the order declared on the class file.
  - `type` should always be the first line in the object.
  - `visibility`, if provided, should be after `type`. If not provided, it is implied to be `public`.
  - `mods`, if provided, should be after `visibility` or `type`.
    - This array must be in the order that the modifiers are declared. For example: `public static final` should be `["static", "final"]`. 
    - The array must be declared on one line.
  - `annotations`, if provided, should be before `value` or `comment`.
  - `value`, if provided, should be before `comment`.
    - The purpose of this field is to document constant values. This only applies to primitive values (`int`, `short`, `long`, `boolean`, etc.) and other values referred as constants (e.g. `String`)
  - `comment` should always be the last line in the object.

### `constructors` array

- This array should always be the fourth object (for enums) or third object (for non-enums) in the JSON file. If not provided, no constructor will be generated.
- This cannot be provided if `class` indicates an interface.
- The objects in this array should be in the order of the constructors declared on the class file.
  - `visibility`, if provided, should be the first line in the object. If not provided, it is implied to be `public`.
  - `params`, if provided, should be after `visibility`.
    - This array must be in the order that the parameters are declared. For example: `Object(int a, int b)` should be `[{"type": "int", "name": "a"}, {"type": "int", "name": "b"}]`.
    - If empty, the constructor should be `Object()`. This is only allowed once.
  - `annotations`, if provided, should be before `comment`.
  - `comment` should always be the last line in the object.

#### `methods` object

Record Classes will automatically generate methods for you according to the constructor with the most parameters.

- This object should always be the fifth object (for enums) or fourth object (for non-enums) in the JSON file. If not provided, no methods will be generated.
- The objects in this object should have their keys be the method name as case sensitive, and must be in the order declared on the class file.
  - `visibility`, if provided, should be the first line in the object. If not provided, it is implied to be `public`.
  - `mods`, if provided, should be after `visibility`.
    - This array must be in the order that the modifiers are declared. For example: `public static final` should be `["static", "final"]`. 
    - The array must be declared on one line.
  - `params`, if provided, should be after `mods` or `visibility` .
    - This array must be in the order that the parameters are declared. For example: `int a, int b` should be `[{"type": "int", "name": "a"}, {"type": "int", "name": "b"}]`. If a parameter has a generic or undescriptive name (e.g. `arg1`), it can be renamed to a more descriptive one, such as the field name it is being mapped to.
  - `generics`, if provided, should be after `params`, `mods`, or `visbility`.
  - `return` should be after `generics`, `params`, `mods`, or `visibility`. If none are provided, it should be the first line in the object. This is also optional if the method returns `void`.
  - `throws` should be after `return`, `generics`, `params`, `mods`, or `visibility`. If none are provided, it should be the first line in the object. This is also optional if the method does not throw any exceptions.
    - This array must be in the order that the exceptions are declared. For example: `throws IOException, IllegalArgumentException` should be `["java.lang.IOException", "java.lang.IllegalArgumentException"]`.
  - `annotations`, if provided, should be before `comment`.
  - `comment` should always be the last line in the object.

Methods that are overrided should have updated documentation (if necessary). Implementing methods for interfaces are optional to document. The documentation for the private field in use for a getter on an interface should have (near) identical documentation to the getter.

##### `methods` object operators

If the method is a getter or setter of a documented field, you can use the `$getter` or `$setter` operator accordingly. The documentation will copy its mods from the field, and will assume it is `public`. For example:
```json
{
  "fields": {
    "foo": {
      "type": "int",
      "comment": "The foo value."
    }
  },
  "methods": {
    "getFoo": {
      "$getter": "foo"
    }
  }
}
```

Fields such as `comment` and `annotations` can be overriden as necessary. Operators should be the first line in the object. For example:
```json
{
  "fields": {
    "foo": {
      "type": "int",
      "comment": "The foo value."
    }
  },
  "methods": {
    "getFoo": {
      "$getter": "foo",
      "visibility": "protected",
      "comment": "Gets the foo value. This also calls a counter that increments the foo value."
    }
  }
}
```

#### Package Info Documentation

Each Package should have a `package-info.json` file, containing a `comment` attribute. This comment should be the first and only line in the file.

#### Subclasses Documentation

For generating JavaDocs for Subclasses, simply provide a Dollar Sign (`$`) for each layer. For Example:

- `Class$Parent$SubclassDocumenting.json`

Additionally, an `enclosing` attribute must be provided before `visibility` in the `class` object.

## Special Characters

The parser has specific special characters since JSON strings cannot use multiple lines.

### HTML/CSS

HTML and CSS is fully supported in the JSON string. Please take special care when using HTML tags.

```json
{
  // ...
  "comment": "This <strong>does not</strong> actually kill the player."
}
```

### Class Links

For linking another java class, either external or external, please provide the full name or type alias for it appended in a `@` and in parenthesis `()`. For example:

```json
{
  // ...
  "comment": "Represents a @(string)."
}

{
  // ...
  "comment": "Spawns a new @(net.minecraft.world.entity.LivingEntity)."
}
```
