package org.gillhub;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Application
{
    static class Point {
        public double lat;
        public double lon;

        public Point(Node node) {
            NamedNodeMap nodeMap = node.getAttributes();
            lat = Double.parseDouble(nodeMap.getNamedItem("lat").getNodeValue());
            lon = Double.parseDouble(nodeMap.getNamedItem("lon").getNodeValue());
        }

        public void updateNode(Node node) {
            NamedNodeMap nodeMap = node.getAttributes();
            nodeMap.getNamedItem("lat").setNodeValue(Double.toString(lat));
            nodeMap.getNamedItem("lon").setNodeValue(Double.toString(lon));
        }

        public void rotateAround(final Point aPoint, final Double angle) {
            double rad = Math.toRadians(angle);
            double dx = lon - aPoint.lon;
            double dy = lat - aPoint.lat;

            double rx = Math.cos(rad)*dx - Math.sin(rad)*dy;
            double ry = Math.sin(rad)*dx + Math.cos(rad)*dy;

            lon = rx + aPoint.lon;
            lat = ry + aPoint.lat;
        }

        public void scaleX(final Point aPoint, final double scale) {
            double dx = lon - aPoint.lon;
            lon = dx*scale + aPoint.lon;
        }

        public void scaleY(final Point aPoint, final double scale) {
            double dy = lat - aPoint.lat;
            lat = dy*scale + aPoint.lat;
        }

        public void moveX(final double dx) {
            lon += dx;
        }

        public void moveY(final double dy) {
            lat += dy;
        }

    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        String filenameIn = null;
        String filenameOut = null;
        for (int argIndex=0; argIndex<args.length; argIndex++) {
            if ("--in".equals(args[argIndex])) {
                filenameIn = args[++argIndex];
            } else if ("--out".equals(args[argIndex])) {
                filenameOut = args[++argIndex];
            }
        }

        if (filenameIn==null || filenameOut==null) {
            System.out.println("Usage: java -jar <file.jar> --in <filename> --out <filename> [--rotate <degrees>] [--movex <offset>] [--movey <offset>] [--scalex <factor>] [--scaley <factor>]");
            System.exit(0);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        File file = new File(filenameIn);
        Document document = builder.parse(file);

        NodeList nodeList = document.getElementsByTagName("trkpt");

        Point aPoint = null;

        for (int i=0; i<nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (aPoint == null) {
                aPoint = new Point(node);
            }

            Point p = new Point(node);

            for (int argIndex=0; argIndex<args.length; argIndex++) {
                if ("--rotate".equals(args[argIndex])) {
                    p.rotateAround(aPoint, Double.parseDouble(args[++argIndex]));
                } else if ("--scalex".equals(args[argIndex])) {
                    p.scaleX(aPoint, Double.parseDouble(args[++argIndex]));
                } else if ("--scaley".equals(args[argIndex])) {
                    p.scaleY(aPoint, Double.parseDouble(args[++argIndex]));
                } else if ("--movex".equals(args[argIndex])) {
                    p.moveX(Double.parseDouble(args[++argIndex]));
                } else if ("--movey".equals(args[argIndex])) {
                    p.moveY(Double.parseDouble(args[++argIndex]));
                }
            }

            p.updateNode(node);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(document);
        FileWriter writer = new FileWriter(filenameOut);
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
    }

}
