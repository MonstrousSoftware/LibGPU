package com.monstrous.math;

import com.monstrous.graphics.PerspectiveCamera;
import com.monstrous.graphics.g3d.BoundingBox;

public class Frustum {
    public Plane[] planes; // top, bottom, left, right, far, near;  plane normals are pointing into the frustum
    public Vector3[] corners;
    private Vector3[] clipSpaceCorners = {
            new Vector3(-1,-1,0), new Vector3(1, -1, 0), new Vector3(1, 1, 0), new Vector3(-1, 1, 0),   // near plane
            new Vector3(-1,-1,1), new Vector3(1, -1, 1), new Vector3(1, 1, 1), new Vector3(-1, 1, 1)   // far plane
    };

    public Frustum() {
        planes = new Plane[6];
        for(int i = 0; i < 6; i++)
            planes[i] = new Plane();
        corners = new Vector3[8];
        for(int i = 0; i < 8; i++)
            corners[i] = new Vector3();
    }


    // todo test this
    public void update(PerspectiveCamera camera){
        Vector3 dir = new Vector3();
        Vector3 point = new Vector3();
        camera.direction.nor();
        camera.up.nor();
        Vector3 right = new Vector3().set(camera.up).crs(camera.direction).nor();
        Vector3 farCentre = new Vector3().set(camera.direction).scl(camera.far).add(camera.position);   //centre of far plane

        // near plane
         point.set(camera.direction).scl(camera.near).add(camera.position);
        planes[5].set( camera.direction, point );

        // far plane
        point.set(camera.direction).scl(camera.far).add(camera.position);
        dir.set(camera.direction).scl(-1);
        planes[4].set(dir, farCentre );

        float aspectRatio = camera.viewportWidth/camera.viewportHeight;
        float halfVside = camera.far * (float)Math.tan(camera.fieldOfView*Math.PI/360f);
        float halfHside = halfVside * aspectRatio;
        point.set(camera.position); // the next 4 planes will go through the camera position

        dir.set(right).scl(-halfHside).add(farCentre).crs(camera.up).scl(-1);   // right
        planes[3].set(dir, point);

        dir.set(right).scl(halfHside).add(farCentre).crs(camera.up);    // left
        planes[2].set(dir, point);

        dir.set(camera.up);
        dir.scl(-halfVside);
        dir.add(farCentre);
        dir.crs(right);
        planes[0].set(dir, point);  // bottom

        dir.set(camera.up).scl(halfVside).add(farCentre).crs(right).scl(-1);        // top
        planes[1].set(dir, point);

        Matrix4 inverse = new Matrix4(camera.combined).inv();
        for(int i = 0; i < 8; i++){
            corners[i].set(clipSpaceCorners[i]).prj(inverse);
        }

    }

    public boolean isInside( Vector3 point ){
        // the planes are pointing inward
        // so if the point is behind any plane it is outside the frustum
        for(Plane plane : planes){
            if (!plane.isInFront(point))
                return false;
        }
        return true;
    }


    public boolean boundsInFrustum(BoundingBox bbox){
        // the planes are pointing inward
        // so if all corner of the bounding box are behind any of the planes, it is not outside the frustum
        Vector3 corner = new Vector3();
        for(Plane plane : planes){
            if(plane.isInFront(corner.set(bbox.min.x, bbox.min.y, bbox.min.z))) continue;
            if(plane.isInFront(corner.set(bbox.max.x, bbox.min.y, bbox.min.z))) continue;
            if(plane.isInFront(corner.set(bbox.min.x, bbox.max.y, bbox.min.z))) continue;
            if(plane.isInFront(corner.set(bbox.max.x, bbox.max.y, bbox.min.z))) continue;
            if(plane.isInFront(corner.set(bbox.min.x, bbox.min.y, bbox.max.z))) continue;
            if(plane.isInFront(corner.set(bbox.max.x, bbox.min.y, bbox.max.z))) continue;
            if(plane.isInFront(corner.set(bbox.min.x, bbox.max.y, bbox.max.z))) continue;
            if(plane.isInFront(corner.set(bbox.max.x, bbox.max.y, bbox.max.z))) continue;
            return false;   // bbox is fully behind this plane
        }
        return true;   // bbox is (at least partially) in front of all planes
    }
}
