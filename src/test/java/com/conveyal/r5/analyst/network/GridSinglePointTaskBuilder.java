package com.conveyal.r5.analyst.network;

import com.conveyal.r5.analyst.FreeFormPointSet;
import com.conveyal.r5.analyst.Grid;
import com.conveyal.r5.analyst.PointSet;
import com.conveyal.r5.analyst.WebMercatorExtents;
import com.conveyal.r5.analyst.cluster.AnalysisWorkerTask;
import com.conveyal.r5.analyst.cluster.TravelTimeSurfaceTask;
import com.conveyal.r5.analyst.decay.StepDecayFunction;
import com.conveyal.r5.api.util.LegMode;
import com.conveyal.r5.api.util.TransitModes;
import org.locationtech.jts.geom.Coordinate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.stream.IntStream;

import static com.conveyal.r5.analyst.WebMercatorGridPointSet.DEFAULT_ZOOM;

/**
 * This creates a task for use in tests. It uses a builder pattern but for a non-immutable task object.
 * It provides convenience methods to set all the necessary fields.
 *
 * NOTE single point tasks can only search out to grids, not freeform pointsets.
 * Those grids are hard-wired into fields of the task, not derived from the pointset object.
 * We may actually want to test with regional tasks to make this less strange, and eventually merge both request types.
 */
public class GridSinglePointTaskBuilder {

    private final GridLayout gridLayout;
    private final AnalysisWorkerTask task;

    public GridSinglePointTaskBuilder (GridLayout gridLayout) {
        this.gridLayout = gridLayout;
        // We will accumulate settings into this task.
        task = new TravelTimeSurfaceTask();
        // Set defaults that can be overridden by calling builder methods.
        task.accessModes = EnumSet.of(LegMode.WALK);
        task.egressModes = EnumSet.of(LegMode.WALK);
        task.directModes = EnumSet.of(LegMode.WALK);
        task.transitModes = EnumSet.allOf(TransitModes.class);
        task.date = LocalDate.of(2020, 1, 1); // Date used by GTFS export TODO make this a constant.
        task.percentiles = new int[] {5, 25, 50, 75, 95};
        // In single point tasks all 121 cutoffs are required (there is a check).
        task.cutoffsMinutes = IntStream.rangeClosed(0, 120).toArray();
        task.decayFunction = new StepDecayFunction();
    }

    public GridSinglePointTaskBuilder setOrigin (int gridX, int gridY) {
        Coordinate origin = gridLayout.getIntersectionLatLon(gridX, gridY);
        task.fromLat = origin.y;
        task.fromLon = origin.x;
        return this;
    }

    public GridSinglePointTaskBuilder setDestination (int gridX, int gridY) {
        Coordinate destination = gridLayout.getIntersectionLatLon(gridX, gridY);
        task.destinationPointSets = new PointSet[] { new FreeFormPointSet(destination) };
        task.destinationPointSetKeys = new String[] { "ID" };
        return this;
    }

    public GridSinglePointTaskBuilder morningPeak () {
        task.fromTime = LocalTime.of(7, 00).toSecondOfDay();
        task.toTime = LocalTime.of(9, 00).toSecondOfDay();
        return this;
    }

    public GridSinglePointTaskBuilder uniformOpportunityDensity (double density) {
        Grid grid = gridLayout.makeUniformOpportunityDataset(density);
        task.destinationPointSets = new PointSet[] { grid };
        task.destinationPointSetKeys = new String[] { "GRID" };

        // In a single point task, the grid of destinations is given with these fields, not from the pointset object.
        // The destination point set (containing the opportunity densities) must then match these same dimensions.
        task.zoom = grid.zoom;
        task.north = grid.north;
        task.west = grid.west;
        task.width = grid.width;
        task.height = grid.height;

        return this;
    }

    public AnalysisWorkerTask build () {
        return task;
    }

}
