package net.sbo.mod.utils.math

import net.minecraft.world.phys.AABB
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Utility functions for raycasting and geometric intersections.
 * The credits to this object go fully to the SkyHanni Mod: https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/RaycastUtils.kt
 */
object RaycastUtils {
    const val EPSILON = 1e-12

    data class Ray(
        val origin: SboVec,
        val direction: SboVec,
    ) {
        init {
            require(direction.isNormalized())
        }
    }

    data class Plane(
        val origin: SboVec,
        val normal: SboVec,
    ) {
        init {
            require(normal.isNormalized())
        }
    }

    /**
     * Intersect an axis-aligned bounding box with a ray.
     * Returns a pair of LorenzVec (entry point, exit point) if the ray hits the box.
     * The entry point may be behind the ray origin if the ray starts inside the box.
     * Returns null if the ray points away from the box or misses it entirely.
     */
    fun intersectAABBWithRay(aabb: AABB, ray: Ray): Pair<SboVec, SboVec>? {
        val aabbMin = SboVec(aabb.minX, aabb.minY, aabb.minZ).toDoubleArray()
        val aabbMax = SboVec(aabb.maxX, aabb.maxY, aabb.maxZ).toDoubleArray()

        val dirArray = ray.direction.toDoubleArray()
        val originArray = ray.origin.toDoubleArray()

        var tmin = -Double.MAX_VALUE
        var tmax = Double.MAX_VALUE

        // Iterate over each axis (x, y, z)
        for (i in 0..2) {
            // If the ray is parallel to the slab (AABB plane pair)
            if (abs(dirArray[i]) < EPSILON) {
                // If the origin is outside the slab, there's no intersection
                if (originArray[i] < aabbMin[i] || originArray[i] > aabbMax[i]) return null
            } else {
                val ood = 1.0 / dirArray[i]
                var t1 = (aabbMin[i] - originArray[i]) * ood
                var t2 = (aabbMax[i] - originArray[i]) * ood

                // Ensure t1 is the intersection with the near plane, and t2 with the far plane
                if (t1 > t2) t1 = t2.also { t2 = t1 }

                // Update tmin and tmax to compute the intersection interval
                tmin = max(tmin, t1)
                tmax = min(tmax, t2)

                // If the interval becomes invalid, there is no intersection
                if (tmin > tmax) return null
            }
        }

        val entry = ray.origin.plus(ray.direction.times(tmin))
        val exit = ray.origin.plus(ray.direction.times(tmax))
        return Pair(entry, exit)
    }

    /**
     * Find the point on a ray where a specific axis has a given value.
     * Axis of 0, 1, 2 is x, y, z respectively
     */
    fun findPointOnRay(ray: Ray, axis: Int, targetValue: Double): SboVec? {
        val originArray = ray.origin.toDoubleArray()
        val dirComponent = ray.direction.toDoubleArray()[axis]

        if (abs(dirComponent) < EPSILON) {
            // Ray parallel to that axis
            return if (abs(originArray[axis] - targetValue) < EPSILON) {
                ray.origin  // Ray is exactly at that coordinate
            } else {
                null  // Can't reach target coordinate
            }
        }

        // Calculate t where point[axis] = targetValue
        val t = (targetValue - originArray[axis]) / dirComponent

        // Calculate full point
        return ray.origin + ray.direction * t
    }

    /**
     * Finds the distance between the given ray and the point. If the point is behind the ray origin (according to the ray's direction),
     * returns [Double.MAX_VALUE] instead.
     */
    fun findDistanceToRay(ray: Ray, point: SboVec): Double {
        val plane = createOrthogonalPlaneToRayAtPoint(ray, point)
        val intersectionPoint = intersectPlaneWithRay(plane, ray)
        if ((intersectionPoint - ray.origin).dotProduct(ray.direction) < 0) return Double.MAX_VALUE
        return intersectionPoint.distance(point)
    }

    fun intersectPlaneWithRay(plane: Plane, ray: Ray): SboVec {
        val intersectionPointDistanceAlongRay =
            (plane.normal.dotProduct(plane.origin) - plane.normal.dotProduct(ray.origin)) / plane.normal.dotProduct(ray.direction)
        return ray.origin + ray.direction.scale(intersectionPointDistanceAlongRay)
    }

    fun createOrthogonalPlaneToRayAtPoint(
        ray: Ray,
        point: SboVec,
    ): Plane {
        return Plane(point, ray.direction)
    }

}