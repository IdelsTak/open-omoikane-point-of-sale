/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * CatalogoArticulos.java
 *
 * Created on 17/08/2008, 05:43:22 PM
 */

package omoikane.formularios;

import java.sql.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;
import omoikane.sistema.*;

/** ////////////////////////////////////////////////////////////////////////////////////////////////
 * /////////////////////////////////////////////////////////////////////////////////////////////////
 * @author Octavio
 */
public class CatalogoArticulos extends javax.swing.JInternalFrame {

    TimerBusqueda          timerBusqueda;
    BufferedImage          fondo;
    public int             IDSeleccionado;
    public String          codigoSeleccionado;
    public int IDAlmacen = omoikane.principal.Principal.IDAlmacen;
    public String          txtQuery;
    public ArticulosTableModel modelo;
    public boolean modal = false;

    public void mostrar() { this.setVisible(true);  }
    public void ocultar() { this.setVisible(false); }

    class TimerBusqueda extends Thread
    {
        CatalogoArticulos ca;
        boolean busquedaActiva = true;

        TimerBusqueda(CatalogoArticulos ca) { this.ca = ca; }
        public void run()
        {
            synchronized(this)
            {
                busquedaActiva = true;
                try { this.wait(500); } catch(Exception e) { Dialogos.lanzarDialogoError(null, "Error en el timer de búsqueda automática", Herramientas.getStackTraceString(e)); }
                if(busquedaActiva && ca.modelo != null) { ca.buscar(); }
            }
        }
        void cancelar()
        {
            busquedaActiva = false;
            try { this.notify(); } catch(Exception e) {}
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        
    }
    /** Creates new form CatalogoArticulos */
    public CatalogoArticulos() {
        //Conectar a MySQL
        try {
            initComponents();
            programarShortcuts();
            cargaProgressBar.setIndeterminate(true);

            new Thread() {
                public void run() {
                    String[]  columnas = {"Código", "Línea", "Grupo", "Concepto", "Unidad", "Precio", "Existencias"};
                    ArrayList cols     = new ArrayList<String>(Arrays.asList(columnas));
                    Class[]   clases   = {String.class, String.class, String.class, String.class, String.class, Double.class, Double.class};
                    ArrayList cls      = new ArrayList<Class>(Arrays.asList(clases));
                    double[]  widths   = {0.16,0.16,0.07,0.41,0.05,0.08,0.06};

                    ArticulosTableModel modeloTabla = new ArticulosTableModel(cols, cls);
                    //jTable1.enableInputMethods(false);
                    
                    modelo = modeloTabla;
                    setQueryTable("select id_articulo as xID, codigo as xCodigo, linea as xLinea, grupo as xGrupo, descripcion as xDescripcion, unidad as xUnidad, precio as xPrecio, existencias as xExistencias " +
                        "from ramcachearticulos");
                    
                    jTable1.setModel(modeloTabla);
                    cargaProgressBar.setIndeterminate(false);

                    Herramientas.setColumnsWidth(jTable1, 964, widths);

                }
            }.start();

            /*setQueryTable("select articulos.id_articulo as xID,articulos.codigo as xCodigo,lineas.descripcion as xLinea,grupos.descripcion as xGrupo,articulos.descripcion as xDescripcion,articulos.unidad as xUnidad,articulos.id_articulo as xIDPrecio,existencias.cantidad as xExistencias " +
                    "from articulos, precios, existencias, lineas , grupos " +
                    "where articulos.id_articulo=precios.id_articulo and precios.id_almacen = "+IDAlmacen+" AND existencias.id_almacen = "+IDAlmacen+" AND existencias.id_articulo = articulos.id_articulo " +
                    "AND lineas.id_linea = articulos.id_linea AND grupos.id_grupo = articulos.id_grupo ");
            */
            /*
            setQueryTable("select articulos.id_articulo as xID,articulos.codigo as xCodigo,lineas.descripcion as xLinea,grupos.descripcion as xGrupo,articulos.descripcion as xDescripcion,articulos.unidad as xUnidad,articulos.id_articulo as xIDPrecioCA,existencias.cantidad as xExistencias" +
                    ", precios.utilidad as xUtilidadCA, articulos.impuestos as xImpuestosCA, precios.costo as xCostoCA, precios.descuento as xDescuentoCA, lineas.descuento as xLineaDescuentoCA, clientes.descuento as xClienteDescuentoCA, grupos.descuento as xGrupoDescuentoCA " +
                    "from articulos, precios, existencias, lineas, clientes, grupos " +
                    "where articulos.id_articulo=precios.id_articulo and precios.id_almacen = "+IDAlmacen+" AND existencias.id_almacen = "+IDAlmacen+" AND existencias.id_articulo = articulos.id_articulo " +
                    "AND clientes.id_cliente = 1 " +
                    "AND lineas.id_linea = articulos.id_linea AND grupos.id_grupo = articulos.id_grupo ");
             * */

            //Instrucciones para el funcionamiento del fondo semistransparente
            this.setOpaque(false);
            ((JPanel)this.getContentPane()).setOpaque(false);
            this.getLayeredPane().setOpaque(false);
            this.getRootPane().setOpaque(false);
            this.generarFondo(this);
            Herramientas.centrarVentana(this);
            this.btnAceptar.setVisible(false);
       } catch(Exception e) {
           Dialogos.error("Error al iniciar catálogo", e);
           this.dispose();
       }

    }
    public void programarShortcuts() {
        /*
            Herramientas.In2ActionX(cat, KeyEvent.VK_ESCAPE, "cerrar"   ) { cat.btnCerrar.doClick()   }
            Herramientas.In2ActionX(cat, KeyEvent.VK_F3    , "buscar"   ) { cat.txtBusqueda.requestFocusInWindow()  }
            Herramientas.In2ActionX(cat, KeyEvent.VK_F4    , "detalles" ) { cat.btnDetalles.doClick() }
            Herramientas.In2ActionX(cat, KeyEvent.VK_F5    , "nuevo"    ) { cat.btnNuevo.doClick()    }
            Herramientas.In2ActionX(cat, KeyEvent.VK_F6    , "modificar") { cat.btnModificar.doClick()}
            Herramientas.In2ActionX(cat, KeyEvent.VK_DELETE, "eliminar" ) { cat.btnEliminar.doClick() }
         */

        Action buscar = new AbstractAction() { public void actionPerformed(ActionEvent e) { txtBusqueda.requestFocusInWindow(); } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "buscar");
        getActionMap().put("buscar"                 , buscar  );

        Action detalles = new AbstractAction() { public void actionPerformed(ActionEvent e) { btnDetalles.doClick(); } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "detalles");
        getActionMap().put("detalles"                 , detalles  );

        Action nuevo = new AbstractAction() { public void actionPerformed(ActionEvent e) { btnNuevo.doClick(); } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "nuevo");
        getActionMap().put("nuevo"                 , nuevo  );

        Action modificar = new AbstractAction() { public void actionPerformed(ActionEvent e) { btnModificar.doClick(); } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "modificar");
        getActionMap().put("modificar"                 , modificar  );

        Action eliminar = new AbstractAction() { public void actionPerformed(ActionEvent e) { btnEliminar.doClick(); } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "eliminar");
        getActionMap().put("eliminar"                 , eliminar  );

        Action imprimir = new AbstractAction() { public void actionPerformed(ActionEvent e) { btnImprimir.doClick(); } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "imprimir");
        getActionMap().put("imprimir"                 , imprimir  );
    }
    public void setModoDialogo()
    {
        modal=true;
        this.btnAceptar.setVisible(true);
        Action aceptar = new AbstractAction() { public void actionPerformed(ActionEvent e) {
            ((CatalogoArticulos)e.getSource()).btnAceptar.doClick();
        } };
        getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "aceptar");
        getActionMap().put("aceptar"                 , aceptar  );
    }

    public void setQueryTable(String query) {
        txtQuery = query;

        modelo.setQuery(query);

    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        txtBusqueda = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnCerrar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnModificar = new javax.swing.JButton();
        btnNuevo = new javax.swing.JButton();
        btnDetalles = new javax.swing.JButton();
        btnImprimir = new javax.swing.JButton();
        btnAceptar = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        chkCodigo = new javax.swing.JCheckBox();
        chkLineas = new javax.swing.JCheckBox();
        chkGrupos = new javax.swing.JCheckBox();
        cargaProgressBar = new javax.swing.JProgressBar();

        setIconifiable(true);
        setTitle("Catálogo de artículos");
        setPreferredSize(new java.awt.Dimension(1000, 590));

        jScrollPane1.setAutoscrolls(true);

        jTable1.setFont(new java.awt.Font("Tahoma", 0, 17));
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Código", "Línea", "Grupo", "Concepto", "Unidad", "Precio", "Existencias"
            }
        ));
        jTable1.setFocusable(false);
        jTable1.setRowHeight(20);
        jTable1.setShowHorizontalLines(false);
        jScrollPane1.setViewportView(jTable1);

        txtBusqueda.setNextFocusableComponent(btnDetalles);
        txtBusqueda.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtBusquedaKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                txtBusquedaKeyTyped(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 12));
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Buscar [F3]:");

        jLabel2.setFont(new java.awt.Font("Arial", 1, 36));
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Catálogo de Artículos");

        btnCerrar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/back.png"))); // NOI18N
        btnCerrar.setText("Cerrar [Esc]");
        btnCerrar.setRequestFocusEnabled(false);
        btnCerrar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarActionPerformed(evt);
            }
        });

        btnEliminar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/page_remove.png"))); // NOI18N
        btnEliminar.setText("Eliminar [Supr]");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        btnModificar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/page_edit.png"))); // NOI18N
        btnModificar.setText("Modificar [F6]");
        btnModificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnModificarActionPerformed(evt);
            }
        });

        btnNuevo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/page_add.png"))); // NOI18N
        btnNuevo.setText("Nuevo [F5]");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });

        btnDetalles.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/page_search.png"))); // NOI18N
        btnDetalles.setText("Detalles [F4]");
        btnDetalles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDetallesActionPerformed(evt);
            }
        });

        btnImprimir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/printer.png"))); // NOI18N
        btnImprimir.setText("<html><center>Imprimir [F8]</center></html>");
        btnImprimir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirActionPerformed(evt);
            }
        });

        btnAceptar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/32x32/accept.png"))); // NOI18N
        btnAceptar.setText("Aceptar [Enter]");
        btnAceptar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAceptarActionPerformed(evt);
            }
        });

        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Critério:");

        chkCodigo.setForeground(new java.awt.Color(255, 255, 255));
        chkCodigo.setSelected(true);
        chkCodigo.setText("Cód, Desc");
        chkCodigo.setOpaque(false);
        chkCodigo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkCodigoActionPerformed(evt);
            }
        });

        chkLineas.setForeground(new java.awt.Color(255, 255, 255));
        chkLineas.setText("Líneas");
        chkLineas.setOpaque(false);
        chkLineas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkLineasActionPerformed(evt);
            }
        });

        chkGrupos.setForeground(new java.awt.Color(255, 255, 255));
        chkGrupos.setText("Grupos");
        chkGrupos.setOpaque(false);
        chkGrupos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkGruposActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 964, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 375, Short.MAX_VALUE)
                        .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnAceptar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDetalles)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnNuevo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnModificar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnEliminar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnImprimir, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, 557, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkCodigo, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkLineas, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkGrupos, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(cargaProgressBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 964, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(btnCerrar, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel6)
                    .addComponent(txtBusqueda, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chkCodigo)
                    .addComponent(chkLineas)
                    .addComponent(chkGrupos))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cargaProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDetalles)
                    .addComponent(btnNuevo)
                    .addComponent(btnModificar)
                    .addComponent(btnEliminar)
                    .addComponent(btnAceptar)
                    .addComponent(btnImprimir, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents



    private void txtBusquedaKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBusquedaKeyTyped
        // TODO add your handling code here:
        //System.out.println("······KeyEvent>"+evt.getKeyChar());
        //System.out.println("······KeyEvent>"+evt.getKeyCode());
        preBuscar(); 
    }//GEN-LAST:event_txtBusquedaKeyTyped

    private void btnCerrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarActionPerformed
        // TODO add your handling code here:

        
        new Thread() {
            public void run() {
                dispose();
                ArticulosTableModel tabModelo = modelo;

                if(tabModelo != null) { tabModelo.destroy(); }
                modelo = null;

                if(!modal){
                ((javax.swing.JInternalFrame)((omoikane.principal.MenuPrincipal)omoikane.principal.Principal.getMenuPrincipal()).getMenuPrincipal()).requestFocusInWindow();
                }
            }
        }.start();

}//GEN-LAST:event_btnCerrarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        // TODO add your handling code here:
        int IDArticulo = ((ScrollableTableModel)jTable1.getModel()).getIDArticuloFila(this.jTable1.getSelectedRow());
        if(IDArticulo != -1) {
            String descripcion = ((ScrollableTableModel)jTable1.getModel()).getDescripcion(jTable1.getSelectedRow());
            if(JOptionPane.showConfirmDialog(null, "¿Realmente desea eliminar éste artículo: \""+descripcion+"\"?", "lala", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                omoikane.principal.Articulos.eliminarArticulo(IDArticulo);
            }
        }
}//GEN-LAST:event_btnEliminarActionPerformed

    private void btnModificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnModificarActionPerformed
        // TODO add your handling code here:
        int IDArticulo = ((ScrollableTableModel)jTable1.getModel()).getIDArticuloFila(this.jTable1.getSelectedRow());

        //Lanzar la ventana de detalles:
        if(IDArticulo != -1) {
            JInternalFrame wnd = (JInternalFrame)omoikane.principal.Articulos.lanzarModificarArticulo(IDArticulo);
            wnd.addInternalFrameListener(iframeAdapter);
        }
}//GEN-LAST:event_btnModificarActionPerformed

    public InternalFrameAdapter iframeAdapter = new InternalFrameAdapter() {
        public void internalFrameClosed(InternalFrameEvent e) { ((ScrollableTableModel)jTable1.getModel()).refrescar(); }
    };

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        // TODO add your handling code here:
        omoikane.principal.Articulos.lanzarFormNuevoArticulo();
}//GEN-LAST:event_btnNuevoActionPerformed

    private void btnDetallesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDetallesActionPerformed
        // TODO add your handling code here:
        int IDArticulo = ((ScrollableTableModel)jTable1.getModel()).getIDArticuloFila(this.jTable1.getSelectedRow());
        
        //Lanzar la ventana de detalles:
        if(IDArticulo != -1) { omoikane.principal.Articulos.lanzarDetallesArticulo(IDArticulo); }
}//GEN-LAST:event_btnDetallesActionPerformed

    private void txtBusquedaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBusquedaKeyPressed
        // TODO add your handling code here:
        if(evt.getKeyCode() == evt.VK_ENTER) {
            this.txtBusqueda.selectAll();
        }
        if(evt.getKeyCode() == evt.VK_DOWN)
        {
            int sigFila = jTable1.getSelectedRow()+1;
            if(sigFila < jTable1.getRowCount())
            {
                this.jTable1.setRowSelectionInterval(sigFila, sigFila);
                this.jTable1.scrollRectToVisible(jTable1.getCellRect(sigFila, 1, true));
            }
        }
        if(evt.getKeyCode() == evt.VK_UP)
        {
            int antFila = jTable1.getSelectedRow()-1;
            if(antFila >= 0) {
                this.jTable1.setRowSelectionInterval(antFila, antFila);
                this.jTable1.scrollRectToVisible(jTable1.getCellRect(antFila, 1, true));
            }
        }
        if(evt.getKeyCode() == evt.VK_PAGE_DOWN)
        {
            int nFilas  = (int) this.jScrollPane1.getViewportBorderBounds().getHeight() / jTable1.getRowHeight();
            int sigFila = jTable1.getSelectedRow()+nFilas;
            if(sigFila > jTable1.getRowCount()) {
                sigFila = jTable1.getRowCount()-1;
            }
            if(sigFila < jTable1.getRowCount()) {
                this.jTable1.setRowSelectionInterval(sigFila, sigFila);
                this.jTable1.scrollRectToVisible(jTable1.getCellRect(sigFila, 1, true));
            }
        }
        if(evt.getKeyCode() == evt.VK_PAGE_UP)
        {
            int nFilas  = (int) this.jScrollPane1.getViewportBorderBounds().getHeight() / jTable1.getRowHeight();
            int antFila = jTable1.getSelectedRow()-nFilas;
            if(antFila < 0) {
                antFila = 0;
            }
            this.jTable1.setRowSelectionInterval(antFila, antFila);
            this.jTable1.scrollRectToVisible(jTable1.getCellRect(antFila, 1, true));
        }
        if(evt.getKeyCode() == evt.VK_DELETE)
        {
            this.btnEliminar.doClick();
        }
    }//GEN-LAST:event_txtBusquedaKeyPressed

    private void btnImprimirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImprimirActionPerformed
        // TODO add your handling code here:
        omoikane.principal.Articulos.lanzarImprimir(this);
    }//GEN-LAST:event_btnImprimirActionPerformed

    private void btnAceptarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAceptarActionPerformed
        // TODO add your handling code here:
        ScrollableTableModel stm = ((ScrollableTableModel)jTable1.getModel());
        int IDArticulo = stm.getIDArticuloFila(this.jTable1.getSelectedRow());
        System.out.println ("btn aceptar");

        if(IDArticulo != -1) { 
            IDSeleccionado = IDArticulo; codigoSeleccionado = (String)stm.getValueAt(this.jTable1.getSelectedRow(), 0);
            this.btnCerrar.doClick();
        }
}//GEN-LAST:event_btnAceptarActionPerformed

    private void chkCodigoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkCodigoActionPerformed
        // TODO add your handling code here:
        buscar();
}//GEN-LAST:event_chkCodigoActionPerformed

    private void chkGruposActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkGruposActionPerformed
        // TODO add your handling code here:
        buscar();
    }//GEN-LAST:event_chkGruposActionPerformed

    private void chkLineasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkLineasActionPerformed
        // TODO add your handling code here:
        buscar();
    }//GEN-LAST:event_chkLineasActionPerformed

    public boolean getBuscarCodigoDescripcion()
    {
        return this.chkCodigo.getModel().isSelected();
    }
    public boolean getBuscarLineas() {
        return this.chkLineas.getModel().isSelected();
    }
    public boolean getBuscarGrupos() {
        return this.chkGrupos.getModel().isSelected();
    }
    public void preBuscar()
    {
        if(timerBusqueda != null && timerBusqueda.isAlive()) { timerBusqueda.cancelar(); }
        this.timerBusqueda = new TimerBusqueda(this);
        timerBusqueda.start();
       
    }

    public void buscar()
    {
        boolean xCodDes = getBuscarCodigoDescripcion();
        boolean xLineas = false; /*getBuscarLineas();*/
        boolean xGrupos = false; /*getBuscarGrupos();*/
        String busqueda = this.txtBusqueda.getText();
        /*
        String query    = "select articulos.id_articulo as xID,articulos.codigo as xCodigo,lineas.descripcion as xLinea,grupos.descripcion as xGrupo,articulos.descripcion as xDescripcion,articulos.unidad as xUnidad,articulos.id_articulo as xIDPrecio,existencias.cantidad as xExistencias " +
                "from articulos, precios, existencias, lineas, grupos " +
                "WHERE precios.id_almacen="+IDAlmacen+" AND existencias.id_almacen="+IDAlmacen+" AND existencias.id_articulo=articulos.id_articulo AND articulos.id_linea = lineas.id_linea AND articulos.id_grupo = grupos.id_grupo AND articulos.id_articulo = precios.id_articulo ";
         * */
        /*
        String query = "select articulos.id_articulo as xID,articulos.codigo as xCodigo,lineas.descripcion as xLinea,grupos.descripcion as xGrupo,articulos.descripcion as xDescripcion,articulos.unidad as xUnidad,articulos.id_articulo as xIDPrecioCA,existencias.cantidad as xExistencias" +
                ", precios.utilidad as xUtilidadCA, articulos.impuestos as xImpuestosCA, precios.costo as xCostoCA, precios.descuento as xDescuentoCA, lineas.descuento as xLineaDescuentoCA, clientes.descuento as xClienteDescuentoCA, grupos.descuento as xGrupoDescuentoCA " +
                "from articulos, precios, existencias, lineas, clientes, grupos " +
                "WHERE clientes.id_cliente=1 AND precios.id_almacen="+IDAlmacen+" AND existencias.id_almacen="+IDAlmacen+" AND existencias.id_articulo=articulos.id_articulo AND articulos.id_linea = lineas.id_linea AND articulos.id_grupo = grupos.id_grupo AND articulos.id_articulo = precios.id_articulo ";
         */
        if(busqueda==null) { xCodDes = xLineas = xGrupos = false; }
        String query = "select DISTINCT a.id_articulo as xID,a.codigo as xCodigo, linea as xLinea, grupo as xGrupo, descripcion as xDescripcion, unidad as xUnidad, precio as xPrecio, existencias as xExistencias" +
                " from ramcachearticulos as a ";

        if(xCodDes) {
            query += ", ramcachecodigos as b ";
        }
        
        if(xCodDes || xLineas || xGrupos) { query += "WHERE "; }
        if(xCodDes) {
                query += " (a.descripcion like '%"+busqueda+"%' or " +
                        "b.codigo like '%"+busqueda+"%') and (a.id_articulo = b.id_articulo) ";
                //query += "(descripcion like '%"+busqueda+"%' or id_articulo in (select id_articulo from ramcachecodigos where codigo like '%"+busqueda+"%')) ";
        }
        if(xCodDes && (xLineas || xGrupos)) { query += "OR "; }
        if(xLineas) {
                query += "(linea like '%"+busqueda+"%' ) ";
        }
        if((xLineas||xCodDes) && xGrupos) { query += "OR "; }
        if(xGrupos) {
                query += "(grupo like '%"+busqueda+"%' ) ";
        }
        //if(xCodDes || xLineas || xGrupos) { query += ")"; }

        
        setQueryTable(query);
    }


    public void paintComponent(Graphics g)
    {
      Graphics2D g2d = (Graphics2D) g;
      g2d.drawImage(fondo, 0, 0, null);

    }
    public void generarFondo(Component componente)
    {
      Rectangle areaDibujo = this.getBounds();
      BufferedImage tmp;
      GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

      tmp = gc.createCompatibleImage(areaDibujo.width, areaDibujo.height,BufferedImage.TRANSLUCENT);
      Graphics2D g2d = (Graphics2D) tmp.getGraphics();
      g2d.setColor(new Color(55,55,255,165));
      g2d.fillRect(0,0,areaDibujo.width,areaDibujo.height);
      fondo = tmp;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAceptar;
    private javax.swing.JButton btnCerrar;
    private javax.swing.JButton btnDetalles;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton btnImprimir;
    private javax.swing.JButton btnModificar;
    private javax.swing.JButton btnNuevo;
    public javax.swing.JProgressBar cargaProgressBar;
    private javax.swing.JCheckBox chkCodigo;
    private javax.swing.JCheckBox chkGrupos;
    private javax.swing.JCheckBox chkLineas;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    public javax.swing.JTable jTable1;
    public javax.swing.JTextField txtBusqueda;
    // End of variables declaration//GEN-END:variables

}

class ArticulosTableModel extends ScrollableTableModel {
    ArticulosTableModel(java.util.List ColNames,ArrayList ColClasses){super(ColNames,ColClasses);}
    public int IDAlmacen = omoikane.principal.Principal.IDAlmacen;

    /*
    public Object getValueAt(int row,int col){
        if(col==5) {

            return (omoikane.sistema.Articulo.precio((super.getValueAt(row, col)),IDAlmacen));

        } else {
            return super.getValueAt(row,col);
        }
    }
     * */
}