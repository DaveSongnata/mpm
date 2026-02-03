package dev.mpm.pom;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading and writing of pom.xml files.
 * Uses Java's built-in XML APIs to preserve formatting as much as possible.
 */
public class PomEditor {

    private final Path pomPath;
    private Document document;

    /**
     * Represents a Maven dependency.
     */
    public static class Dependency {
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final String scope;

        public Dependency(String groupId, String artifactId, String version, String scope) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
        }

        @Override
        public String toString() {
            String result = groupId + ":" + artifactId + "@" + version;
            if (scope != null && !scope.equals("compile")) {
                result += " (" + scope + ")";
            }
            return result;
        }
    }

    public PomEditor(Path pomPath) {
        this.pomPath = pomPath;
    }

    /**
     * Checks if pom.xml exists.
     */
    public boolean exists() {
        return Files.exists(pomPath);
    }

    /**
     * Loads and parses the pom.xml file.
     */
    public void load() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(pomPath.toFile());
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse pom.xml: " + e.getMessage(), e);
        }
    }

    /**
     * Saves the pom.xml file.
     */
    public void save() throws IOException {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            // Preserve existing XML declaration
            document.setXmlStandalone(true);

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(pomPath.toFile());
            transformer.transform(source, result);

            // Clean up extra blank lines that transformer adds
            cleanupFormatting();
        } catch (TransformerException e) {
            throw new IOException("Failed to save pom.xml: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all dependencies from the pom.xml.
     */
    public List<Dependency> getDependencies() throws IOException {
        if (document == null) {
            load();
        }

        List<Dependency> dependencies = new ArrayList<>();
        NodeList depNodes = document.getElementsByTagName("dependency");

        for (int i = 0; i < depNodes.getLength(); i++) {
            Node depNode = depNodes.item(i);
            if (depNode.getNodeType() == Node.ELEMENT_NODE) {
                Element depElement = (Element) depNode;

                // Only process direct dependencies (not in dependencyManagement)
                Node parent = depElement.getParentNode();
                if (parent != null && parent.getNodeName().equals("dependencies")) {
                    Node grandParent = parent.getParentNode();
                    if (grandParent != null && grandParent.getNodeName().equals("project")) {
                        String groupId = getChildText(depElement, "groupId");
                        String artifactId = getChildText(depElement, "artifactId");
                        String version = getChildText(depElement, "version");
                        String scope = getChildText(depElement, "scope");

                        if (groupId != null && artifactId != null) {
                            dependencies.add(new Dependency(groupId, artifactId, version, scope));
                        }
                    }
                }
            }
        }

        return dependencies;
    }

    /**
     * Checks if a dependency already exists.
     */
    public boolean hasDependency(String groupId, String artifactId) throws IOException {
        for (Dependency dep : getDependencies()) {
            if (dep.groupId.equals(groupId) && dep.artifactId.equals(artifactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a new dependency to the pom.xml.
     *
     * @param groupId    the group ID
     * @param artifactId the artifact ID
     * @param version    the version
     * @param scope      the scope (compile, test, provided, runtime, or null for default)
     * @return true if added, false if already exists
     */
    public boolean addDependency(String groupId, String artifactId, String version, String scope) throws IOException {
        if (document == null) {
            load();
        }

        // Check if already exists
        if (hasDependency(groupId, artifactId)) {
            return false;
        }

        // Find or create <dependencies> element
        Element dependenciesElement = getOrCreateDependenciesElement();

        // Create new <dependency> element
        Element dependency = document.createElement("dependency");

        Element groupIdElement = document.createElement("groupId");
        groupIdElement.setTextContent(groupId);
        dependency.appendChild(groupIdElement);

        Element artifactIdElement = document.createElement("artifactId");
        artifactIdElement.setTextContent(artifactId);
        dependency.appendChild(artifactIdElement);

        Element versionElement = document.createElement("version");
        versionElement.setTextContent(version);
        dependency.appendChild(versionElement);

        if (scope != null && !scope.equals("compile")) {
            Element scopeElement = document.createElement("scope");
            scopeElement.setTextContent(scope);
            dependency.appendChild(scopeElement);
        }

        // Add indentation (text node with newline and tabs)
        dependenciesElement.appendChild(document.createTextNode("\n        "));
        dependenciesElement.appendChild(dependency);
        dependenciesElement.appendChild(document.createTextNode("\n    "));

        return true;
    }

    /**
     * Removes a dependency from the pom.xml.
     *
     * @param groupId    the group ID
     * @param artifactId the artifact ID
     * @return true if removed, false if not found
     */
    public boolean removeDependency(String groupId, String artifactId) throws IOException {
        if (document == null) {
            load();
        }

        NodeList depNodes = document.getElementsByTagName("dependency");

        for (int i = 0; i < depNodes.getLength(); i++) {
            Node depNode = depNodes.item(i);
            if (depNode.getNodeType() == Node.ELEMENT_NODE) {
                Element depElement = (Element) depNode;

                String depGroupId = getChildText(depElement, "groupId");
                String depArtifactId = getChildText(depElement, "artifactId");

                if (groupId.equals(depGroupId) && artifactId.equals(depArtifactId)) {
                    Node parent = depElement.getParentNode();

                    // Remove preceding whitespace
                    Node prevSibling = depElement.getPreviousSibling();
                    if (prevSibling != null && prevSibling.getNodeType() == Node.TEXT_NODE) {
                        parent.removeChild(prevSibling);
                    }

                    parent.removeChild(depElement);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates a new basic pom.xml file.
     */
    public void createNew(String groupId, String artifactId, String version) throws IOException {
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "    <groupId>" + groupId + "</groupId>\n" +
                "    <artifactId>" + artifactId + "</artifactId>\n" +
                "    <version>" + version + "</version>\n" +
                "    <packaging>jar</packaging>\n" +
                "\n" +
                "    <properties>\n" +
                "        <maven.compiler.source>11</maven.compiler.source>\n" +
                "        <maven.compiler.target>11</maven.compiler.target>\n" +
                "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "    </properties>\n" +
                "\n" +
                "    <dependencies>\n" +
                "    </dependencies>\n" +
                "</project>\n";

        Files.writeString(pomPath, content);

        // Reload the document
        load();
    }

    /**
     * Gets the text content of a child element.
     */
    private String getChildText(Element parent, String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        if (children.getLength() > 0) {
            return children.item(0).getTextContent().trim();
        }
        return null;
    }

    /**
     * Gets or creates the <dependencies> element.
     */
    private Element getOrCreateDependenciesElement() {
        NodeList dependenciesList = document.getElementsByTagName("dependencies");

        // Find the direct child of <project>
        for (int i = 0; i < dependenciesList.getLength(); i++) {
            Element deps = (Element) dependenciesList.item(i);
            Node parent = deps.getParentNode();
            if (parent != null && parent.getNodeName().equals("project")) {
                return deps;
            }
        }

        // Create new <dependencies> element
        Element root = document.getDocumentElement();
        Element dependencies = document.createElement("dependencies");
        root.appendChild(document.createTextNode("\n\n    "));
        root.appendChild(dependencies);
        root.appendChild(document.createTextNode("\n"));

        return dependencies;
    }

    /**
     * Cleans up formatting issues caused by the XML transformer.
     */
    private void cleanupFormatting() throws IOException {
        String content = Files.readString(pomPath);

        // Remove multiple consecutive blank lines
        content = content.replaceAll("(\r?\n){3,}", "\n\n");

        // Ensure consistent line endings
        content = content.replace("\r\n", "\n");

        Files.writeString(pomPath, content);
    }
}
