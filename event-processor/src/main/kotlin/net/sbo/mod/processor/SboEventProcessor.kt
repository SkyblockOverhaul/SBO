package net.sbo.mod.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.OutputStreamWriter

class SboEventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("net.sbo.mod.utils.events.annotations.SboEvent")
        val generatedObjects = mutableSetOf<String>()

        val classSymbols = symbols.filterIsInstance<KSFunctionDeclaration>()
            .groupBy { it.parentDeclaration as? KSClassDeclaration }

        classSymbols.forEach { (clazz, functions) ->
            if (clazz == null) return@forEach
            val packageName = clazz.packageName.asString()
            val className = clazz.simpleName.asString()
            val fileName = "${className}_SboEventRegister"

            generatedObjects.add("$packageName.$fileName")

            val isObject = clazz.classKind == ClassKind.OBJECT
            if (!isObject) {
                logger.error("@SboEvent can only be used inside objects: ${clazz.simpleName.asString()}", clazz)
                return@forEach
            }

            val isCompanionObject = clazz.classKind == ClassKind.OBJECT && clazz.parentDeclaration is KSClassDeclaration

            val instanceRef = if (isCompanionObject) {
                val enclosingClass = clazz.parentDeclaration as KSClassDeclaration
                "${enclosingClass.simpleName.asString()}.Companion"
            } else {
                className
            }

            logger.info("Generating EventRegister for $packageName.$className with functions: ${functions.joinToString { it.simpleName.asString() }}")

            // Only collect KSFile objects that are not null
            val dependencies = functions.mapNotNull { it.containingFile }.distinct().toTypedArray<KSFile>()
            val file = codeGenerator.createNewFile(
                Dependencies(true, *dependencies),
                packageName,
                fileName
            )

            OutputStreamWriter(file).use { writer ->
                val functionCalls = functions.joinToString("\n") { fn ->
                    val param = fn.parameters.firstOrNull()
                    if (null == param || 1 != fn.parameters.size) {
                        logger.error(
                            "@SboEvent functions must have exactly one parameter",
                            fn
                        )
                        return@joinToString ""
                    }
                    val paramType = param.type.resolve().declaration.qualifiedName?.asString()
                    val priority = fn.annotations.find { it.annotationType.resolve().declaration.qualifiedName?.asString() == "net.sbo.mod.utils.events.annotations.SboEvent" }
                        ?.arguments?.find { it.name?.getShortName() == "priority" }
                        ?.value?.toString()?.toIntOrNull() ?: 1
                    if (paramType == null) {
                        logger.error("Cannot resolve type for ${fn.simpleName.asString()}")
                        return@joinToString "// Cannot resolve type for ${fn.simpleName.asString()}"
                    } else {
                        "SBOEvent.on($paramType::class, $priority) { e -> $instanceRef.${fn.simpleName.asString()}(e) }"
                    }
                }

                writer.write("""
                    package $packageName

                    import net.sbo.mod.utils.events.SBOEvent
                    import net.sbo.mod.utils.events.annotations.SboEvent

                    object $fileName {
                        fun register() {
                            $functionCalls
                        }
                    }
                """.trimIndent())
            }
        }

        if (generatedObjects.isNotEmpty()) {
            val allDependencies: List<KSFile> = classSymbols.keys.mapNotNull { it?.containingFile }
            val registryFile = codeGenerator.createNewFile(
                Dependencies(true, *allDependencies.toTypedArray()),
                "net.sbo.mod.utils.events",
                "SboEventGeneratedRegistry"
            )

            OutputStreamWriter(registryFile).use { writer ->
                writer.write(
                    """
                    package net.sbo.mod.utils.events
        
                    object SboEventGeneratedRegistry {
                        fun registerAll() {
                            ${generatedObjects.joinToString("\n") { "$it.register()" }}
                        }
                    }
                """.trimIndent()
                )
            }
        }
        return emptyList()
    }
}
