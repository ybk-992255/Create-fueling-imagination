package com.fuelingimagination.client.armor;

import com.fuelingimagination.FuelingImagination;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class ArmorModeModel implements ResourceManagerReloadListener {
   public static final ArmorModeModel INSTANCE = new ArmorModeModel();
   private static final ResourceLocation MODEL = new ResourceLocation("fuelingimagination", "models/armor/armor_mode.json");
   private static final String[] FACES = new String[]{"down", "up", "north", "south", "west", "east"};
   private static final int[] WINDING = new int[]{0, 3, 2, 1};
   private volatile List<ArmorModeModel.Part> parts = List.of();

   public void onResourceManagerReload(ResourceManager resources) {
      try (Reader reader = resources.openAsReader(MODEL)) {
         JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
         JsonArray elements = model.getAsJsonArray("elements");
         List<ArmorModeModel.Part> loaded = new ArrayList<>();

         for (JsonElement node : model.getAsJsonArray("groups")) {
            JsonObject group = node.getAsJsonObject();
            String sourceName = group.get("name").getAsString();

            String bone = switch (sourceName) {
               case "left_arm" -> "right_arm";
               case "right_arm" -> "left_arm";
               case "left_leg", "left_foot" -> "right_leg";
               case "right_leg", "right_foot" -> "left_leg";
               default -> sourceName;
            };
            Vector3f pivot = vector(group.getAsJsonArray("origin"));
            if (sourceName.endsWith("_foot")) {
               pivot.y = 12.0F;
            }

            List<ArmorModeModel.Quad> quads = new ArrayList<>();
            readGroup(group, elements, new Matrix4f(), pivot, quads);
            loaded.add(new ArmorModeModel.Part(bone, List.copyOf(quads)));
         }

         this.parts = List.copyOf(loaded);
      } catch (Exception var15) {
         this.parts = List.of();
         FuelingImagination.LOGGER.error("Could not load armor mode geometry", var15);
      }
   }

   public void render(PlayerModel<?> model, PoseStack pose, VertexConsumer vertices, int light) {
      for (ArmorModeModel.Part part : this.parts) {
         String var8 = part.bone;

         ModelPart limb = switch (var8) {
            case "head" -> model.head;
            case "body" -> model.body;
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            default -> null;
         };
         if (limb != null) {
            pose.pushPose();
            limb.translateAndRotate(pose);
            pose.scale(0.0625F, 0.0625F, 0.0625F);

            for (ArmorModeModel.Quad quad : part.quads) {
               for (int corner : WINDING) {
                  Vector3f v = quad.vertices[corner];
                  int uvCorner = corner + quad.uvRotation & 3;
                  float u = quad.uv[uvCorner != 0 && uvCorner != 1 ? 2 : 0];
                  float t = quad.uv[uvCorner != 0 && uvCorner != 3 ? 3 : 1];
                  vertices.vertex(pose.last().pose(), v.x, v.y, v.z)
                     .color(255, 255, 255, 255)
                     .uv(u, t)
                     .overlayCoords(OverlayTexture.NO_OVERLAY)
                     .uv2(light)
                     .normal(pose.last().normal(), quad.normal.x, quad.normal.y, quad.normal.z)
                     .endVertex();
               }
            }

            pose.popPose();
         }
      }
   }

   private static void readGroup(JsonObject group, JsonArray elements, Matrix4f parent, Vector3f limbPivot, List<ArmorModeModel.Quad> quads) {
      Matrix4f transform = new Matrix4f(parent);
      if (group.has("rotation")) {
         Vector3f pivot = vector(group.getAsJsonArray("origin"));
         Vector3f rotation = vector(group.getAsJsonArray("rotation")).mul((float) (Math.PI / 180.0));
         transform.translate(pivot).rotateZYX(rotation.z, rotation.y, rotation.x).translate(new Vector3f(pivot).negate());
      }

      for (JsonElement child : group.getAsJsonArray("children")) {
         if (child.isJsonObject()) {
            readGroup(child.getAsJsonObject(), elements, transform, limbPivot, quads);
         } else {
            readCube(elements.get(child.getAsInt()).getAsJsonObject(), transform, limbPivot, quads);
         }
      }
   }

   private static void readCube(JsonObject cube, Matrix4f parent, Vector3f limbPivot, List<ArmorModeModel.Quad> output) {
      Vector3f a = vector(cube.getAsJsonArray("from"));
      Vector3f b = vector(cube.getAsJsonArray("to"));
      Matrix4f transform = new Matrix4f(parent);
      if (cube.has("rotation")) {
         JsonObject rotation = cube.getAsJsonObject("rotation");
         Vector3f pivot = vector(rotation.getAsJsonArray("origin"));
         float angle = (float)Math.toRadians((double)rotation.get("angle").getAsFloat());
         String axis = rotation.get("axis").getAsString();
         transform.translate(pivot);
         switch (axis) {
            case "x":
               transform.rotateX(angle);
               break;
            case "y":
               transform.rotateY(angle);
               break;
            case "z":
               transform.rotateZ(angle);
               break;
            default:
               throw new IllegalArgumentException("Unknown cube axis " + axis);
         }

         if (rotation.has("rescale") && rotation.get("rescale").getAsBoolean()) {
            float scale = 1.0F / (float)Math.cos((double)angle);
            transform.scale(axis.equals("x") ? 1.0F : scale, axis.equals("y") ? 1.0F : scale, axis.equals("z") ? 1.0F : scale);
         }

         transform.translate(new Vector3f(pivot).negate());
      }

      float[][][] points = new float[][][]{
         {{a.x, a.y, b.z}, {a.x, a.y, a.z}, {b.x, a.y, a.z}, {b.x, a.y, b.z}},
         {{a.x, b.y, a.z}, {a.x, b.y, b.z}, {b.x, b.y, b.z}, {b.x, b.y, a.z}},
         {{b.x, b.y, a.z}, {b.x, a.y, a.z}, {a.x, a.y, a.z}, {a.x, b.y, a.z}},
         {{a.x, b.y, b.z}, {a.x, a.y, b.z}, {b.x, a.y, b.z}, {b.x, b.y, b.z}},
         {{a.x, b.y, a.z}, {a.x, a.y, a.z}, {a.x, a.y, b.z}, {a.x, b.y, b.z}},
         {{b.x, b.y, b.z}, {b.x, a.y, b.z}, {b.x, a.y, a.z}, {b.x, b.y, a.z}}
      };
      JsonObject faces = cube.getAsJsonObject("faces");

      for (int i = 0; i < FACES.length; i++) {
         if (faces.has(FACES[i])) {
            JsonObject face = faces.getAsJsonObject(FACES[i]);
            if (face.has("texture") && !face.get("texture").isJsonNull()) {
               Vector3f[] positions = new Vector3f[4];

               for (int j = 0; j < 4; j++) {
                  positions[j] = transform.transformPosition(new Vector3f(points[i][j])).sub(limbPivot);
                  positions[j].y *= -1.0F;
               }

               Vector3f normal = new Vector3f(positions[3]).sub(positions[0]).cross(new Vector3f(positions[2]).sub(positions[0])).normalize();
               float[] uv = new float[4];

               for (int j = 0; j < 4; j++) {
                  uv[j] = face.getAsJsonArray("uv").get(j).getAsFloat() / 16.0F;
               }

               int rotation = face.has("rotation") ? face.get("rotation").getAsInt() / 90 : 0;
               output.add(new ArmorModeModel.Quad(positions, uv, normal, rotation));
            }
         }
      }
   }

   private static Vector3f vector(JsonArray array) {
      return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
   }

   private static record Part(String bone, List<ArmorModeModel.Quad> quads) {
   }

   private static record Quad(Vector3f[] vertices, float[] uv, Vector3f normal, int uvRotation) {
   }
}
