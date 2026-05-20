import { useEffect, useRef } from "react";

const palette = ["rgba(53, 200, 183, 0.4)", "rgba(56, 142, 220, 0.38)", "rgba(220, 244, 238, 0.24)"];

const InteractiveBackground = () => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    let frameId;
    let time = 0;

    const resize = () => {
      canvas.width = canvas.parentElement.clientWidth;
      canvas.height = canvas.parentElement.clientHeight;
    };

    const draw = () => {
      const width = canvas.width;
      const height = canvas.height;
      ctx.clearRect(0, 0, width, height);
      ctx.lineWidth = 1;

      for (let i = 0; i < 18; i += 1) {
        const y = (height / 18) * i + Math.sin(time * 0.008 + i) * 12;
        const alpha = 0.12 + (i % 3) * 0.04;
        ctx.strokeStyle = palette[i % palette.length].replace(/[\d.]+\)$/u, `${alpha})`);
        ctx.beginPath();
        ctx.moveTo(0, y);
        for (let x = 0; x <= width; x += 48) {
          ctx.lineTo(x, y + Math.sin(time * 0.01 + x * 0.012 + i) * 10);
        }
        ctx.stroke();
      }

      for (let i = 0; i < 10; i += 1) {
        const x = ((i + 1) / 11) * width + Math.sin(time * 0.006 + i) * 18;
        ctx.strokeStyle = "rgba(220, 244, 238, 0.1)";
        ctx.beginPath();
        ctx.moveTo(x, height * 0.12);
        ctx.lineTo(x + Math.cos(time * 0.006 + i) * 32, height * 0.88);
        ctx.stroke();
      }

      time += 1;
      frameId = requestAnimationFrame(draw);
    };

    resize();
    window.addEventListener("resize", resize);
    draw();

    return () => {
      window.removeEventListener("resize", resize);
      cancelAnimationFrame(frameId);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      style={{
        position: "absolute",
        inset: 0,
        zIndex: 1,
        pointerEvents: "none",
        mixBlendMode: "screen",
        opacity: 0.8,
      }}
    />
  );
};

export default InteractiveBackground;
