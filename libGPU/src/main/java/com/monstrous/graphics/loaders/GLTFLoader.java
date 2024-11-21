package com.monstrous.graphics.loaders;

import com.monstrous.graphics.Color;
import com.monstrous.graphics.loaders.gltf.*;
import com.monstrous.math.Quaternion;
import com.monstrous.math.Vector3;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


// JSON parser of the GLTF file format into a set of GLTF class objects

public class GLTFLoader {

    public static GLTF load(String filePath) {
        int slash = filePath.lastIndexOf('/');
        String path = filePath.substring(0, slash + 1);
        String name = filePath.substring(slash + 1);
        MaterialData materialData = null;
        GLTF gltf = new GLTF();

        String contents;
        try {
            contents = Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Could not read GLTF file "+filePath);
        }

        JSONObject file = (JSONObject)JSONValue.parse(contents);




        JSONArray ims = (JSONArray)file.get("images");
        if(ims != null) {
            System.out.println("images: " + ims.size());
            for (int i = 0; i < ims.size(); i++) {
                JSONObject image = (JSONObject) ims.get(i);
                String imagepath = (String) image.get("uri");
                System.out.println("image path: " + imagepath);
                // load texture file
                GLTFImage im = new GLTFImage();
                im.uri = path + imagepath;
                gltf.images.add(im);
            }


            JSONArray sampls = (JSONArray) file.get("samplers");
            if(sampls != null) {
                System.out.println("samplers: " + sampls.size());
                for (int i = 0; i < sampls.size(); i++) {
                    GLTFSampler sampler = new GLTFSampler();

                    JSONObject smpl = (JSONObject) sampls.get(i);
                    sampler.name = (String) smpl.get("name");
                    Long wrapS = (Long) smpl.get("wrapS");
                    sampler.wrapS = wrapS == null ? 10497 : wrapS.intValue();
                    Long wrapT = (Long) smpl.get("wrapS");
                    sampler.wrapT = wrapT == null ? 10497 : wrapT.intValue();

                    gltf.samplers.add(sampler);
                }
            }
        }

        JSONArray textures = (JSONArray)file.get("textures");
        if (textures != null) {
            System.out.println("textures: " + textures.size());
            for (int i = 0; i < textures.size(); i++) {
                GLTFTexture texture = new GLTFTexture();

                JSONObject tex = (JSONObject) textures.get(i);
                texture.name = (String) tex.get("name");
                texture.source = getInt(tex, "source", 0);
                texture.sampler = getInt(tex, "sampler", 0);

                gltf.textures.add(texture);
            }
        }

        JSONArray mats = (JSONArray)file.get("materials");
        if (mats != null) {


            System.out.println("materials: " + mats.size());
            for (int i = 0; i < mats.size(); i++) {

                GLTFMaterialPBR pbr = new GLTFMaterialPBR();
                GLTFMaterial material = new GLTFMaterial();

                JSONObject mat = (JSONObject) mats.get(i);
                material.name = (String) mat.get("name");

                JSONObject pbrMR = (JSONObject) mat.get("pbrMetallicRoughness");
                JSONObject base = (JSONObject) pbrMR.get("baseColorTexture");
                if(base != null)
                    pbr.baseColorTexture = getInt(base, "index", 0);
                JSONArray bc = (JSONArray)pbrMR.get("baseColorFactor");
                if(bc != null){
                    pbr.baseColorFactor = getColor(bc);
                }

                JSONObject metal = (JSONObject) pbrMR.get("metallicRoughnessTexture");
                if(metal != null) {
                    long metalIndex = (Long) metal.get("index");
                    pbr.metallicRoughnessTexture = (int) metalIndex;
                }

                JSONObject normalMap = (JSONObject) mat.get("normalTexture");
                if(normalMap != null)
                    material.normalTexture = getInt(normalMap, "index", -1);

                JSONObject emissive = (JSONObject) mat.get("emissiveTexture");
                if(emissive != null)
                    material.emissiveTexture = getInt(emissive, "index", -1);

                JSONObject occlusion = (JSONObject) mat.get("occlusionTexture");
                if(occlusion != null)
                    material.occlusionTexture = getInt(occlusion, "index", -1);


                material.pbrMetallicRoughness = pbr;
                gltf.materials.add(material);
            }
        }

        JSONArray meshes = (JSONArray)file.get("meshes");
        System.out.println("meshes: "+meshes.size());
        for(int i = 0; i < meshes.size(); i++){
            GLTFMesh mesh = new GLTFMesh();

            JSONObject m = (JSONObject)meshes.get(i);
            mesh.name = (String)m.get("name");
            JSONArray primitives = (JSONArray)m.get("primitives");
            for(int j = 0; j < primitives.size(); j++){
                GLTFPrimitive primitive = new GLTFPrimitive();

                JSONObject p = (JSONObject)primitives.get(j);

                Long mode = (Long) p.get("mode");
                primitive.mode = (mode == null ? 4 : mode.intValue());
                Long indices = (Long)p.get("indices");
                primitive.indices = (indices == null ? 0 : indices.intValue());
                Long material = (Long)p.get("material");
                primitive.material = (material == null ? 0 : material.intValue());

                JSONObject attribs = (JSONObject)p.get("attributes");
                for(Object key : attribs.keySet()) {
                    String attributeName = (String) key;
                    Long attributeValue = (Long) attribs.get(key);
                    GLTFAttribute attribute = new GLTFAttribute(attributeName, attributeValue.intValue());
                    primitive.attributes.add(attribute);
                }

                mesh.primitives.add(primitive);
            }
            gltf.meshes.add(mesh);
        }

        JSONArray buffers = (JSONArray)file.get("buffers");
        System.out.println("buffers: "+buffers.size());
        for(int i = 0; i < buffers.size(); i++){
            GLTFBuffer buffer = new GLTFBuffer();

            JSONObject buf = (JSONObject)buffers.get(i);
            buffer.name = (String)buf.get("name");
            buffer.uri = path + (String)buf.get("uri");
            Long len = (Long)buf.get("byteLength");
            buffer.byteLength = (len == null ? 0 : len.intValue());

            gltf.buffers.add(buffer);
        }

        JSONArray bufferViews = (JSONArray)file.get("bufferViews");
        System.out.println("buffer views: "+bufferViews.size());
        for(int i = 0; i < bufferViews.size(); i++){
            GLTFBufferView bufferView = new GLTFBufferView();

            JSONObject bufView = (JSONObject)bufferViews.get(i);
            bufferView.name = (String)bufView.get("name");
            Long buffer = (Long)bufView.get("buffer");
            bufferView.buffer = (buffer == null ? 0 : buffer.intValue());
            Long offset = (Long)bufView.get("byteOffset");
            bufferView.byteOffset = (offset == null ? 0 : offset.intValue());
            Long len = (Long)bufView.get("byteLength");
            bufferView.byteLength = (len == null ? 0 : len.intValue());
            Long stride = (Long)bufView.get("byteStride");
            bufferView.byteStride = (stride == null ? 0 : stride.intValue());
            Long target = (Long)bufView.get("target");
            bufferView.target = (target == null ? 0 : target.intValue());

            gltf.bufferViews.add(bufferView);
        }

        JSONArray accessors = (JSONArray)file.get("accessors");
        System.out.println("accessors: "+accessors.size());
        for(int i = 0; i < accessors.size(); i++){
            GLTFAccessor accessor = new GLTFAccessor();

            JSONObject ac = (JSONObject)accessors.get(i);
            accessor.name = (String)ac.get("name");
            Long bufferView = (Long)ac.get("bufferView");
            accessor.bufferView = (bufferView == null ? 0 : bufferView.intValue());
            Long offset = (Long)ac.get("byteOffset");
            accessor.byteOffset = (offset == null ? 0 : offset.intValue());
            Long ct = (Long)ac.get("componentType");
            accessor.componentType = (ct == null ? 0 : ct.intValue());
            String n = (String)ac.get("normalized");
            accessor.normalized = (n == null ? false : (n.contentEquals("true") ? true : false));
            Long count = (Long)ac.get("count");
            accessor.count = (count == null ? 0 : count.intValue());
            accessor.type = (String)ac.get("type");


            gltf.accessors.add(accessor);
        }

        JSONArray nodes = (JSONArray)file.get("nodes");
        System.out.println("nodes: "+accessors.size());
        for(int i = 0; i < nodes.size(); i++){
            GLTFNode node = new GLTFNode();

            JSONObject nd = (JSONObject)nodes.get(i);
            node.name = (String)nd.get("name");
            node.camera = getInt(nd, "camera", -1);
            node.skin = getInt(nd, "skin", -1);
            node.mesh = getInt(nd, "mesh", -1);

            JSONArray tr = (JSONArray)nd.get("translation");
            if(tr != null){
                node.translation = getVector3(tr);
            }
            JSONArray sc = (JSONArray)nd.get("scale");
            if(sc != null){
                node.scale = getVector3(sc);
            }
            JSONArray rot = (JSONArray)nd.get("rotation");
            if(rot != null){
                node.rotation = getRotation(rot);
            }
            JSONArray ch = (JSONArray)nd.get("children");
            if(ch != null){
                for(int j = 0; j < ch.size(); j++){
                    Long id = (Long)ch.get(j);
                    node.children.add(id.intValue());
                }
            }

            gltf.nodes.add(node);
        }

        JSONArray scenes = (JSONArray)file.get("scenes");
        System.out.println("scenes: "+accessors.size());
        for(int i = 0; i < scenes.size(); i++){
            GLTFScene scene = new GLTFScene();

            JSONObject sc = (JSONObject)scenes.get(i);
            scene.name = (String)sc.get("name");
            JSONArray nds = (JSONArray)sc.get("nodes");
            for(int j= 0 ; j < nds.size(); j++){
                Long nodeId = (Long)nds.get(j);
                scene.nodes.add(nodeId.intValue());
            }
            gltf.scenes.add(scene);
        }

        gltf.scene = getInt(file, "scene", 0);
        System.out.println("scene: "+gltf.scene);

        return gltf;
    }

    private static int getInt(JSONObject obj, String key, int fallback){
        Long value = (Long)obj.get(key);
        return (value == null ? fallback : value.intValue());
    }

    private static Vector3 getVector3(JSONArray obj) {
        if (obj.size() != 3)
            throw new RuntimeException("GLTF: Expected vector with 3 elements");
        Number x = (Number) obj.get(0);
        Number y = (Number) obj.get(1);
        Number z = (Number) obj.get(2);
        return  new Vector3(x.floatValue(), y.floatValue(), z.floatValue());
    }

    private static Color getColor(JSONArray obj) {
        if (obj.size() != 4)
            throw new RuntimeException("GLTF: Expected color with 4 elements");
        Number r = (Number) obj.get(0);
        Number g = (Number) obj.get(1);
        Number b = (Number) obj.get(2);
        Number a = (Number) obj.get(3);
        return  new Color(r.floatValue(), g.floatValue(), b.floatValue(), a.floatValue());
    }


    private static Quaternion getRotation(JSONArray rot) {
        if(rot.size() != 4)
            throw new RuntimeException("GLTF: Expected node.rotation with 4 elements");
        Number x = (Number)rot.get(0);
        Number y = (Number)rot.get(1);
        Number z = (Number)rot.get(2);
        Number w = (Number)rot.get(3);
        return new Quaternion(x.floatValue(), y.floatValue(), z.floatValue(), w.floatValue());
    }




}
