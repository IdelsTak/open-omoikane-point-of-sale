package omoikane.caja.business;

import groovy.util.Eval;
import javafx.collections.ObservableList;
import name.antonsmirnov.javafx.dialog.Dialog;
import omoikane.caja.data.IProductosDAO;
import omoikane.caja.handlers.StockIssuesHandler;
import omoikane.caja.presentation.BuscarMasDummyProducto;
import omoikane.caja.presentation.CajaController;
import omoikane.caja.presentation.CajaModel;
import omoikane.caja.presentation.ProductoModel;
import omoikane.entities.Caja;
import omoikane.entities.LegacyVenta;
import omoikane.entities.LegacyVentaDetalle;
import omoikane.principal.Principal;
import omoikane.principal.Sucursales;
import omoikane.producto.Producto;
import omoikane.repository.CajaRepo;
import omoikane.repository.VentaRepo;
import omoikane.sistema.Comprobantes;
import omoikane.sistema.Dialogos;
import omoikane.sistema.Nadesico;
import omoikane.sistema.Usuarios;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.synyx.hades.domain.PageRequest;
import org.synyx.hades.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Octavio
 * Date: 13/09/12
 * Time: 02:01 AM
 * To change this template use File | Settings | File Templates.
 */


public class CajaLogicImpl implements ICajaLogic {
    public static Logger logger = Logger.getLogger(CajaLogicImpl.class);
    Boolean capturaBloqueada = false;

    @Autowired
    IProductosDAO productosDAO;

    @Autowired
    Comprobantes comprobantes;

    @Autowired
    VentaRepo ventaRepo;

    @Autowired
    CajaRepo cajaRepo;

    @PersistenceContext
    EntityManager entityManager;
    private CajaController controller;

    /**
     * Pseudo evento gatillado cuando se intenta capturar un producto en la "línea de captura".
     * Ignora cualquier intento de captura si ya existe una en curso
     */
    public synchronized void onCaptura(CajaModel model) {
        if(!capturaBloqueada) {
            capturaBloqueada = true;
            try {
                LineaDeCapturaFilter capturaFilter = new LineaDeCapturaFilter(model.getCaptura().get());
                model.getCaptura().set("");

                addProducto(model, capturaFilter);

            } catch(IndexOutOfBoundsException e) {
                logger.trace("Producto no encontrado");
            } catch (Exception e) {
                logger.error("Error durante captura ('evento onCaptura')", e);
            }
            capturaBloqueada = false;
        }
    }

    public void buscar(CajaModel model) {
        Pageable pagina = model.getPaginacionBusqueda();
        String descripcion = model.getCaptura().get();
        if(descripcion.isEmpty()) return;
        ArrayList<Producto> productos = (ArrayList<Producto>) productosDAO.findByDescripcionLike( "%"+descripcion+"%", pagina);
        ObservableList<ProductoModel> obsProductos = model.getProductos();

        if (pagina.getPageNumber()==0 )
            obsProductos.clear();
        else
            obsProductos.remove( obsProductos.size() - 1 ); //Remueve el renglón "Buscar más productos"

        for( Producto p : productos ) {
            ProductoModel productoModel = new ProductoModel();
            productoToProductoModel(p, productoModel);
            productoModel.cantidadProperty().set( new BigDecimal(1) );

            obsProductos.add(productoModel);
        }
        if(obsProductos.size() > 0) obsProductos.add( new BuscarMasDummyProducto() );

        model.setPaginacionBusqueda(new PageRequest(pagina.getPageNumber()+1, pagina.getPageSize()));
    }


    private void addProducto(CajaModel model, LineaDeCapturaFilter capturaFilter) throws Exception {
        model.getProductos().clear(); // Borra resultados de la búsqueda integrada

        Producto producto = productosDAO.findByCodigo(capturaFilter.getCodigo()).get(0);
        /*Articulo producto = productoRepo.findByCodigo(captura.getCodigo()).get(0);*/

        ProductoModel productoModel = new ProductoModel();
        productoToProductoModel(producto, productoModel);
        productoModel.cantidadProperty().set( capturaFilter.getCantidad() );
        reglasDeCantidad(productoModel);

        //Agrupar o agregar
        Boolean       agrupar = false;
        ProductoModel productoBase = null;
        for ( ProductoModel p : model.getVenta() ) {
            if(p.getId().get() == productoModel.getId().get()) { agrupar = true; productoBase = p; break; }
        }
        if(agrupar) {
            BigDecimal cantidadBase  = productoBase.cantidadProperty().get();
            BigDecimal nuevaCantidad = cantidadBase.add(productoModel.cantidadProperty().get());
            productoModel.cantidadProperty().set( nuevaCantidad );
            model.getVenta().remove(productoBase);
            model.getVenta().add(productoModel);
        }   else {
            model.getVenta().add(productoModel);
        }
        persistirVenta();
    }

    @Override
    public void deleteRowFromVenta(int row) {
        getController().getModel().getVenta().remove(row);
        getVentaAbiertaBean().getItems().remove(row);
        persistirVenta();
    }

    @Override
    public void persistirVenta() {
        CajaModel model = getController().getModel();
        ventaAbiertaBean = guardarVenta(model);
    }

    private void reglasDeCantidad(ProductoModel productoModel) throws Exception {
        try {
            String unidad = productoModel.getProductoData().getUnidad();
            BigDecimal cantidad = productoModel.cantidadProperty().get();

            if(!unidad.equalsIgnoreCase("KG") && !unidad.equalsIgnoreCase("LT"))
            { productoModel.cantidadProperty().set( cantidad.setScale(0, RoundingMode.CEILING) ); }
            else
            {
                if(cantidad.compareTo(new BigDecimal("0.025")) < 0)
                    productoModel.cantidadProperty().set(new BigDecimal("0.025"));
            }
        } catch(Exception e) {
            throw new Exception("Error en caja comprobando unidad de producto", e);
        }
    }

    private void productoToProductoModel(Producto producto, ProductoModel productoModel) {
        productoModel.getId()             .set( producto.getId() );
        productoModel.codigoProperty()    .set( producto.getCodigo() );
        productoModel.precioBaseProperty().set( producto.getPrecio().getPrecioBase() );
        productoModel.conceptoProperty()  .set( producto.getDescripcion() );
        productoModel.descuentoProperty() .set( producto.getPrecio().getDescuento() );
        productoModel.impuestosProperty() .set( producto.getPrecio().getImpuestos() );
        productoModel.precioProperty()    .set( producto.getPrecio().getPrecio() );
        productoModel.setProductoData(producto);
    }


    public void calcularCambio(CajaModel model )
    {
        BigDecimal efectivo = model.getEfectivo().get();
        BigDecimal total    = model.getTotal().get();
        BigDecimal cambio   = efectivo.subtract(total);
        model.getCambio().setValue( cambio );
    }

    @Transactional
    public void terminarVenta(CajaModel model) {

        BigDecimal ventaTotal = model.getTotal().get();
        if( ventaTotal.compareTo( new BigDecimal("0.10") ) > 0 ) {
            try {
                ventaAbiertaBean.setCompletada(true);
                LegacyVenta venta = guardarVenta(model);

                //Hace las salidas de inventario / Make inventory issues
                new StockIssuesHandler(getController()).handle();

                imprimirVenta(venta);

                Dialog.showInfo("Venta registrada",
                        "Venta registrada");

                nuevaVenta();
            } catch (Exception e) {
                logger.error("Error al guardar venta, venta no registrada.", e);
                throw new RuntimeException("prueba");
            }
        }

    }

    private LegacyVenta ventaAbiertaBean;
    @Override
    public LegacyVenta getVentaAbiertaBean() { return ventaAbiertaBean; }

    @Override
    public void nuevaVenta() {

        instanciarModeloVenta();
        getController().getCapturaTextField().requestFocus();
    }


    private void instanciarModeloVenta() {
        LegacyVenta ventaIncompleta = buscarVentaIncompleta();
        getController().setModel( new CajaModel() );

        if(ventaIncompleta == null) {
            ventaAbiertaBean = null;
            persistirVenta();
        } else {
            cargarVentaIncompleta(ventaIncompleta);
            ventaAbiertaBean = ventaIncompleta;
        }
    }

    private LegacyVenta buscarVentaIncompleta() {
        Integer idCaja    = Principal.IDCaja;
        LegacyVenta venta = ventaRepo.findByIdCajaAndCompletada(idCaja, false);
        return venta;
    }

    private void cargarVentaIncompleta(LegacyVenta venta) {

        for( LegacyVentaDetalle lvd : venta.getItems()) {
            ProductoModel productoModel = new ProductoModel();
            productoToProductoModel(productosDAO.findById(new Long(lvd.getIdArticulo())), productoModel);
            productoModel.cantidadProperty().set(BigDecimal.valueOf(lvd.getCantidad()));
            getController().getModel().getVenta().add(productoModel);
        }
    }

    private void imprimirVenta(LegacyVenta venta) {
        comprobantes.ticketVenta(venta.getId()); //imprimir ticket
        comprobantes.imprimir();
    }

    @Transactional
    private LegacyVenta guardarVenta(CajaModel model) {
        Integer idCaja    = Principal.IDCaja;
        Integer idAlmacen = Principal.IDAlmacen;
        Integer idUsuario = Usuarios.getIDUsuarioActivo();
        Double  efectivo  = model.getEfectivo().get().doubleValue();
        Double  cambio    = model.getCambio().get().doubleValue();
        Date    fechaHora = (Date) entityManager.createNativeQuery("SELECT current_timestamp").getSingleResult();

        LegacyVenta venta;
        if(ventaAbiertaBean == null) {
            Integer folio     = asignarFolio(idCaja);
            venta = new LegacyVenta();
            venta.setCompletada(false);
            venta.setFolio(folio);
        } else {
            venta = ventaAbiertaBean;
        }

        venta.setIdCliente ( 1 );
        venta.setIdUsuario(idUsuario);
        venta.setIdAlmacen(idAlmacen);
        venta.setIdCaja(idCaja);
        venta.setEfectivo(efectivo);
        venta.setCambio(cambio);
        venta.setCentecimosredondeados(0d);
        venta.setFacturada(0);
        venta.setFechaHora ( fechaHora );
        venta.setDescuento (model.getDescuento().get().doubleValue());
        venta.setSubtotal  (model.getSubtotal().get().doubleValue());
        venta.setImpuestos (model.getImpuestos().getValue().doubleValue());
        venta.setTotal     (model.getTotal().get().doubleValue());

        int i = 0;
        List<LegacyVentaDetalle> itemsTmp = venta.getItems();
        venta.setItems(null);

        for (ProductoModel producto : model.getVenta()) {
            LegacyVentaDetalle lvd;
            if(itemsTmp == null || itemsTmp.size() <= i)
                    lvd = new LegacyVentaDetalle();
                else
                    lvd = itemsTmp.get(i);

            lvd.setIdAlmacen ( idAlmacen );
            lvd.setIdArticulo( producto.getLongId().intValue() );
            lvd.setIdCaja    ( idCaja );
            lvd.setIdLinea   ( producto.getProductoData().getLineaByLineaId().getId() );
            lvd.setCantidad  ( producto.cantidadProperty().get().doubleValue() );
            lvd.setPrecio    ( producto.precioProperty().get().doubleValue() );
            lvd.setDescuento ( producto.descuentoProperty().get().doubleValue() );
            lvd.setImpuestos ( producto.impuestosProperty().get().doubleValue() );
            lvd.setSubtotal  ( producto.getSubtotal().doubleValue() );
            lvd.setTotal     ( producto.getImporte().doubleValue() );
            lvd.setTipoSalida( "" );

            venta.addItem(lvd);
            i++;
        }

        venta = ventaRepo.saveAndFlush(venta);
        return venta;
    }

    public Integer asignarFolio(Integer idCaja) {
        //Query q = entityManager.createQuery("SELECT Caja.uFolio where id_caja = ?" );
        Caja caja = entityManager.find(Caja.class, idCaja);

        Integer folioActual = caja.getUFolio();
        folioActual++;
        caja.setUFolio( folioActual );
        return folioActual;
    }

    public void onVentaListChanged(CajaModel model) {
        BigDecimal subtotal = new BigDecimal(0);
        subtotal.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal descuentos = new BigDecimal(0);
        descuentos.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal impuestos = new BigDecimal(0);
        impuestos.setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal total = new BigDecimal(0);
        total.setScale(2, BigDecimal.ROUND_HALF_UP);

        for ( ProductoModel producto : model.getVenta() ) {
            subtotal   = subtotal  .add( producto.getSubtotal() );
            descuentos = descuentos.add( producto.descuentoProperty().get() );
            impuestos  = impuestos .add( producto.impuestosProperty().get() );
        }

        total = total.add( subtotal );
        total = total.subtract( descuentos );
        total = total.add( impuestos );

        model.getSubtotal().set( subtotal );
        model.getDescuento().set( descuentos );
        model.getImpuestos().set( impuestos );
        model.getTotal().set( total );
    }

    @Override
    public void setController(CajaController cajaController) {
        this.controller = cajaController;
    }

    public CajaController getController() {
        return controller;
    }
}
