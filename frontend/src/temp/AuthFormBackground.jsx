import { useEffect, useRef } from "react";


const AuthFormBackground = () => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    let animationFrameId;
    let time = 0;

    const resizeCanvas = () => {
      canvas.width = canvas.parentElement.clientWidth;
      canvas.height = canvas.parentElement.clientHeight;
    };

    window.addEventListener("resize", resizeCanvas);
    resizeCanvas();

    
    const blobs = [
      {
        color: { r: 76, g: 221, b: 205 },
        baseX: 0.72, baseY: 0.16,
        radiusX: 0.22, radiusY: 0.14,
        size: 0.7,
        speed: 0.0001,
        phase: 0,
      },
      {
        color: { r: 72, g: 161, b: 226 },
        baseX: 0.62, baseY: 0.5,
        radiusX: 0.15, radiusY: 0.2,
        size: 0.6,
        speed: 0.00012,
        phase: Math.PI,
      },
      {
        color: { r: 174, g: 241, b: 232 },
        baseX: 0.84, baseY: 0.35,
        radiusX: 0.15, radiusY: 0.25,
        size: 0.56,
        speed: 0.00008,
        phase: Math.PI * 0.5,
      },
    ];

    const drawBlob = (blob, t) => {
      const w = canvas.width;
      const h = canvas.height;

      
      const cx = (blob.baseX + Math.sin(t * blob.speed + blob.phase) * blob.radiusX) * w;
      const cy = (blob.baseY + Math.cos(t * blob.speed * 0.8 + blob.phase) * blob.radiusY) * h;

      const radius = blob.size * Math.min(w, h);

      
      const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius);
      gradient.addColorStop(0, `rgba(${blob.color.r}, ${blob.color.g}, ${blob.color.b}, 0.28)`);
      gradient.addColorStop(0.5, `rgba(${blob.color.r}, ${blob.color.g}, ${blob.color.b}, 0.12)`);
      gradient.addColorStop(1, `rgba(${blob.color.r}, ${blob.color.g}, ${blob.color.b}, 0)`);

      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, w, h);
    };

    const animate = () => {
      const w = canvas.width;
      const h = canvas.height;

      ctx.clearRect(0, 0, w, h);

      
      ctx.fillStyle = "#0c2d3b";
      ctx.fillRect(0, 0, w, h);

      ctx.globalCompositeOperation = "screen";
      blobs.forEach((blob) => drawBlob(blob, time));
      ctx.globalCompositeOperation = "source-over";

      
      const fadeGradient = ctx.createLinearGradient(0, 0, w * 0.5, 0);
      fadeGradient.addColorStop(0, "rgba(12, 45, 59, 0.92)");
      fadeGradient.addColorStop(0.32, "rgba(12, 45, 59, 0.68)");
      fadeGradient.addColorStop(0.64, "rgba(12, 45, 59, 0.24)");
      fadeGradient.addColorStop(1, "rgba(12, 45, 59, 0)");
      ctx.fillStyle = fadeGradient;
      ctx.fillRect(0, 0, w * 0.5, h);

      time += 16;
      animationFrameId = requestAnimationFrame(animate);
    };

    animate();

    return () => {
      window.removeEventListener("resize", resizeCanvas);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: "absolute",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        zIndex: 0,
        pointerEvents: "none",
      }}
    />
  );
};

export default AuthFormBackground;
