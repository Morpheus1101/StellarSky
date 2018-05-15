package stellarium.render.stellars;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import stellarium.StellarSkyResources;
import stellarium.client.ClientSettings;
import stellarium.render.shader.IShaderObject;
import stellarium.render.shader.IUniformField;
import stellarium.render.shader.ShaderHelper;
import stellarium.render.stellars.access.EnumStellarPass;
import stellarium.render.stellars.atmosphere.AtmosphereRenderer;
import stellarium.render.stellars.atmosphere.AtmosphereSettings;
import stellarium.render.stellars.atmosphere.EnumAtmospherePass;
import stellarium.render.stellars.atmosphere.FramebufferCustom;
import stellarium.render.stellars.layer.LayerRI;
import stellarium.render.stellars.phased.StellarPhasedRenderer;
import stellarium.util.OpenGlUtil;
import stellarium.util.RenderUtil;

public enum StellarRenderer {
	INSTANCE;

	private FramebufferCustom sky, temporary;
	private int prevWidth = 0, prevHeight = 0;
	private int prevFramebufferBound;

	private IShaderObject hdrToldr, linearToSRGB;
	private IUniformField fieldBr;

	private int maxLevel;
	private FloatBuffer brightness;

	public void initialize(ClientSettings settings) {
		AtmosphereSettings atmSettings = (AtmosphereSettings) settings.getSubConfig(AtmosphereSettings.KEY);
		AtmosphereRenderer.INSTANCE.initialize(atmSettings);

		this.brightness = ByteBuffer.allocateDirect(3 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		this.setupShader();
	}

	public void setupFramebuffer(int width, int height) {
		this.sky = FramebufferCustom.builder()
				.setupTexFormat(OpenGlUtil.RGB16F, GL11.GL_RGB, OpenGlUtil.TEXTURE_FLOAT)
				.setupDepthStencil(true, false)
				.build(width, height);

		this.temporary = FramebufferCustom.builder()
				.setupTexFormat(OpenGlUtil.RGB16F, GL11.GL_RGB, OpenGlUtil.TEXTURE_FLOAT)
				.setupDepthStencil(false, false)
				.build(width, height);
	}

	public void setupShader() {
		this.hdrToldr = ShaderHelper.getInstance().buildShader("HDRtoLDR",
				StellarSkyResources.vertexHDRtoLDR, StellarSkyResources.fragmentHDRtoLDR);
		hdrToldr.getField("texture").setInteger(0);
		this.fieldBr = hdrToldr.getField("brightness");

		this.linearToSRGB = ShaderHelper.getInstance().buildShader("linearToSRGB",
				StellarSkyResources.vertexLinearToSRGB, StellarSkyResources.fragmentLinearToSRGB);
		linearToSRGB.getField("texture").setInteger(0);
	}

	public void preRender(ClientSettings settings, StellarRI info) {
		AtmosphereSettings atmSettings = (AtmosphereSettings) settings.getSubConfig(AtmosphereSettings.KEY);
		AtmosphereRenderer.INSTANCE.preRender(atmSettings, info);

		Framebuffer mcBuffer = info.minecraft.getFramebuffer();
		if(mcBuffer.framebufferWidth != this.prevWidth || mcBuffer.framebufferHeight != this.prevHeight) {
			this.setupFramebuffer(mcBuffer.framebufferWidth, mcBuffer.framebufferHeight);
			int ct
			this.prevWidth = mcBuffer.framebufferWidth;
			this.prevHeight = mcBuffer.framebufferHeight;
		}
	}

	private static final StellarTessellator tessellator = new StellarTessellator();

	public void preProcess() {
		// TODO AA change to manual rendering
		// TODO AA Try to lessen the Brightness Gap
		// TODO AA Just use vector to specify position

		this.prevFramebufferBound = GlStateManager.glGetInteger(OpenGlUtil.FRAMEBUFFER_BINDING);

		sky.bindFramebuffer(false);
		GlStateManager.depthMask(true);
		sky.framebufferClear();
		GlStateManager.depthMask(false);
	}

	public void postProcess(StellarRI info) {
		brightness.clear();
		float br = (brightness.get(0) + brightness.get(1) + brightness.get(2)) / 3.0f;
		br = 1.0f;

		// HDR to LDR
		temporary.bindFramebuffer(true);
		temporary.framebufferClear();

		sky.bindFramebufferTexture();
		OpenGlUtil.generateMipmap(GL11.GL_TEXTURE_2D);
		GL11.glGetTexImage(target, level, format, type, pixels);

		hdrToldr.bindShader();
		fieldBr.setDouble(br);
		RenderUtil.renderFullQuad();
		hdrToldr.releaseShader();


		// Linear RGB to sRGB
		OpenGlUtil.bindFramebuffer(OpenGlUtil.FRAMEBUFFER_GL, this.prevFramebufferBound);

		linearToSRGB.bindShader();
		temporary.bindFramebufferTexture();
		RenderUtil.renderFullQuad();
		linearToSRGB.releaseShader();
	}

	public void render(StellarModel model, StellarRI info) {
		LayerRI layerInfo = new LayerRI(info, tessellator);

		// Pre-process
		this.preProcess();

		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.Prepare, info);

		// Setup surface scatter
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.SetupSurfaceScatter, info);
		// Render surface scatter
		StellarPhasedRenderer.INSTANCE.render(model.layersModel, EnumStellarPass.SurfaceScatter, layerInfo);

		// Setup point scatter
		OpenGlUtil.enablePointSprite();
		GlStateManager.shadeModel(GL11.GL_FLAT);
		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.SetupPointScatter, info);
		// Render point scatter
		StellarPhasedRenderer.INSTANCE.render(model.layersModel, EnumStellarPass.PointScatter, layerInfo);

		// Setup opaque
		OpenGlUtil.disablePointSprite();
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.shadeModel(GL11.GL_SMOOTH);
		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.SetupOpaque, info);
		// Render opaque
		StellarPhasedRenderer.INSTANCE.render(model.layersModel, EnumStellarPass.Opaque, layerInfo);

		// Setup opaque scatter
		GlStateManager.depthMask(false);
		GlStateManager.disableDepth();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
		GlStateManager.shadeModel(GL11.GL_FLAT);
		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.SetupOpaqueScatter, info);
		// Render opaque scatter
		StellarPhasedRenderer.INSTANCE.render(model.layersModel, EnumStellarPass.OpaqueScatter, layerInfo);

		// Prepare dominate scatter
		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.SetupDominateScatter, info);
		// Render dominate scatter
		StellarPhasedRenderer.INSTANCE.render(model.layersModel, EnumStellarPass.DominateScatter, layerInfo);

		// Finalize dominate scatter
		AtmosphereRenderer.INSTANCE.render(model.atmModel, EnumAtmospherePass.Finalize, info);

		// Post-process
		this.postProcess(info);

		// State setup
		GlStateManager.depthMask(true);
		GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
		GlStateManager.enableDepth();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}
}
