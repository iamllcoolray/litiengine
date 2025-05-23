package de.gurkenlabs.litiengine.resources;

import de.gurkenlabs.litiengine.entities.IEntity;
import de.gurkenlabs.litiengine.environment.MapObjectSerializer;
import de.gurkenlabs.litiengine.environment.tilemap.IMap;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObject;
import de.gurkenlabs.litiengine.environment.tilemap.IMapObjectLayer;
import de.gurkenlabs.litiengine.environment.tilemap.IMapOrientation;
import de.gurkenlabs.litiengine.environment.tilemap.ITileLayer;
import de.gurkenlabs.litiengine.environment.tilemap.ITileset;
import de.gurkenlabs.litiengine.environment.tilemap.xml.MapObjectLayer;
import de.gurkenlabs.litiengine.environment.tilemap.xml.Tile;
import de.gurkenlabs.litiengine.environment.tilemap.xml.TileData;
import de.gurkenlabs.litiengine.environment.tilemap.xml.TileLayer;
import de.gurkenlabs.litiengine.environment.tilemap.xml.TmxException;
import de.gurkenlabs.litiengine.environment.tilemap.xml.TmxMap;
import de.gurkenlabs.litiengine.graphics.RenderType;
import de.gurkenlabs.litiengine.util.io.FileUtilities;
import de.gurkenlabs.litiengine.util.io.XmlUtilities;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A container class for managing map resources. This class extends the ResourcesContainer class, specifically for IMap objects.
 */
public final class Maps extends ResourcesContainer<IMap> {
  private static final Logger log = Logger.getLogger(Maps.class.getName());

  Maps() {
  }

  /**
   * Checks if the specified file name has a supported extension.
   *
   * @param fileName The name of the file to check.
   * @return true if the file has a supported extension, false otherwise.
   */
  public static boolean isSupported(String fileName) {
    String extension = FileUtilities.getExtension(fileName);
    return extension.equalsIgnoreCase(TmxMap.FILE_EXTENSION);
  }

  /**
   * Starts a process that allows the generation of maps from code.
   * <p>
   * Notice that you must call this within a try-with block or ensure that {@link MapGenerator#close()} is called before using the generated map
   * instance.
   * <p>
   *
   * <b>Example usage:</b>
   *
   * <pre>
   * IMap map;
   * try (MapGenerator generator = Resources.maps().generate("name", 50, 50, 16, 16, Resources.tilesets().get("tileset.tsx"))) {
   *   ITileLayer tileLayer = generator.addTileLayer(RenderType.GROUND, (x, y) -&gt; {
   *     if (x == y) {
   *       // draw a diagonal in another tile color
   *       return 2;
   *     }
   *
   *     // fill the entire map with this tile
   *     return 1;
   *   });
   *
   *   // set an explicit tile at a location
   *   tileLayer.setTile(10, 10, 3);
   *
   *   // add a collision box to the map
   *   generator.add(new CollisionBox(0, 64, 100, 10));
   *
   *   map = generator.getMap();
   * }
   * </pre>
   *
   * @param orientation The orientation of the map to be generated.
   * @param name        The name of the map to be generated.
   * @param width       The width (in tiles).
   * @param height      The height (in tiles).
   * @param tileWidth   The width of a tile (in pixels).
   * @param tileHeight  The height of a tile (in pixels).
   * @param tilesets    Tilesets that will be used by the map.
   * @return A {@code MapGenerator} instance used to add additional layers or objects to the map.
   */
  public MapGenerator generate(IMapOrientation orientation, String name, int width, int height, int tileWidth, int tileHeight, ITileset... tilesets) {
    TmxMap map = new TmxMap(orientation);
    map.setTileWidth(tileWidth);
    map.setTileHeight(tileHeight);
    map.setWidth(width);
    map.setHeight(height);
    map.setName(name);

    for (ITileset tileset : tilesets) {
      map.getTilesets().add(tileset);
    }

    return new MapGenerator(map);
  }

  /**
   * Loads an IMap resource from the specified URL.
   *
   * @param resourceName The URL of the resource to load.
   * @return The loaded IMap resource.
   * @throws IOException        If an I/O error occurs.
   * @throws URISyntaxException If the URL is not formatted correctly.
   */
  @Override
  protected IMap load(URL resourceName) throws IOException, URISyntaxException {
    TmxMap map;
    try {
      map = XmlUtilities.read(TmxMap.class, resourceName);
    } catch (JAXBException e) {
      throw new TmxException(e.getMessage(), e);
    }

    if (map == null) {
      return null;
    }
    map.finish(resourceName);
    return map;
  }

  @Override
  protected String getAlias(String resourceName, IMap resource) {
    if (resource == null || resource.getName() == null || resource.getName().isEmpty() || resource.getName().equalsIgnoreCase(resourceName)) {
      return null;
    }

    return resource.getName();
  }

  /**
   * This class provides the API to simplify the generation of map resources from code.
   */
  public class MapGenerator implements AutoCloseable {
    private final TmxMap map;

    private MapGenerator(TmxMap map) {
      this.map = map;
    }

    /**
     * Gets the map generated by this instance.
     * <p>
     * Make sure this instance is closed before using the map in your game.
     * </p>
     *
     * @return The map generated by this instance.
     * @see #close()
     */
    public IMap getMap() {
      return this.map;
    }

    /**
     * Adds a new tile tile layer to the generated map of this instance.
     *
     * <b>Example for a tileCallback:</b>
     *
     * <pre>
     * (x, y) -&gt; {
     *   if (x == y) {
     *     // draw a diagonal in another tile color
     *     return 2;
     *   }
     *
     *   // fill the entire map with this tile
     *   return 1;
     * }
     * </pre>
     *
     * @param renderType   The rendertype of the added layer.
     * @param tileCallback The callback that defines which tile gid will be assigned at the specified x, y grid coordinates.
     * @return The newly added tile layer.
     */
    public ITileLayer addTileLayer(RenderType renderType, IntBinaryOperator tileCallback) {
      List<Tile> tiles = new ArrayList<>();
      for (int y = 0; y < this.map.getHeight(); y++) {
        for (int x = 0; x < this.map.getWidth(); x++) {
          int tile = tileCallback.applyAsInt(x, y);
          tiles.add(new Tile(tile));
        }
      }

      TileData data;
      try {
        data = new TileData(tiles, this.map.getWidth(), this.map.getHeight(), TileData.Encoding.CSV, TileData.Compression.NONE);
        data.setValue(TileData.encode(data));
      } catch (IOException e) {
        log.log(Level.SEVERE, e.getMessage(), e);
        return null;
      }

      TileLayer layer = new TileLayer(data);
      layer.setRenderType(renderType);
      layer.setWidth(this.map.getWidth());
      layer.setHeight(this.map.getHeight());

      this.map.addLayer(layer);
      return layer;
    }

    /**
     * Adds a {@code MapObject} created by the specified entity to the map of this instance.
     * <p>
     * If no layer has been added yet, a default {@code MapObjectLayer} will be created by this method.
     * </p>
     *
     * @param entity The entity to be converted to a map object and added to the first {@code MapObjectLayer} of the generated map.
     * @return The created map object.
     */
    public IMapObject add(IEntity entity) {
      return this.add(MapObjectSerializer.serialize(entity));
    }

    /**
     * Adds a {@code MapObject} created by the specified entity to the map of this instance.
     *
     * @param layer  The layer to which the map object will be added.
     * @param entity The entity to be converted to a map object and added to the specified {@code MapObjectLayer}.
     * @return The created map object.
     */
    public IMapObject add(IMapObjectLayer layer, IEntity entity) {
      IMapObject mapObject = MapObjectSerializer.serialize(entity);
      return this.add(layer, mapObject);
    }

    /**
     * Adds the specified map object to the map of this instance.
     * <p>
     * If no layer has been added yet, a default {@code MapObjectLayer} will be created by this method.
     * </p>
     *
     * @param mapObject The mapObject to be added to the first {@code MapObjectLayer} of the generated map.
     * @return The added map object.
     */
    public IMapObject add(IMapObject mapObject) {
      IMapObjectLayer layer;
      if (this.getMap().getMapObjectLayers().isEmpty()) {
        layer = new MapObjectLayer();
        layer.setName(MapObjectLayer.DEFAULT_MAPOBJECTLAYER_NAME);
        this.getMap().addLayer(layer);
      } else {
        layer = this.getMap().getMapObjectLayer(0);
      }

      return this.add(layer, mapObject);
    }

    /**
     * Adds the specified map object to the map of this instance.
     *
     * @param layer     The layer to which the map object will be added.
     * @param mapObject The mapObject to be added to the specified {@code MapObjectLayer}.
     * @return The added map object.
     */
    public IMapObject add(IMapObjectLayer layer, IMapObject mapObject) {
      layer.addMapObject(mapObject);
      return mapObject;
    }

    /**
     * <b>It is crucial to call this before using the generated map of this instance.</b><br>
     * <p>
     * This will call the {@code finish} method on the map instance and make sure that the generated map is available over the resources API.
     * </p>
     *
     * @see TmxMap#finish(URL)
     */
    @Override
    public void close() {
      try {
        URL resource = Resources.getLocation(map.getName() + "." + TmxMap.FILE_EXTENSION);
        this.map.finish(resource);

        Maps.this.add(resource, this.map);
      } catch (TmxException e) {
        log.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

}
