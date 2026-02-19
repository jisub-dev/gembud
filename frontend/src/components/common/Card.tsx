import { HTMLAttributes, forwardRef } from 'react';
import { cn } from '@/utils/cn';

interface CardProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'highlighted';
  hover?: boolean;
}

/**
 * Gaming-style card with neon border and glow effect.
 */
const Card = forwardRef<HTMLDivElement, CardProps>(
  ({ className, variant = 'default', hover = true, children, ...props }, ref) => {
    const baseStyles = 'bg-dark-secondary rounded-card p-card-padding border transition-all duration-200';

    const variantStyles = {
      default: 'border-dark-tertiary',
      highlighted: 'border-neon-purple/50 shadow-glow-purple',
    };

    const hoverStyles = hover
      ? 'hover:border-neon-purple hover:shadow-glow-purple hover:-translate-y-1 cursor-pointer'
      : '';

    return (
      <div
        ref={ref}
        className={cn(
          baseStyles,
          variantStyles[variant],
          hoverStyles,
          className
        )}
        {...props}
      >
        {children}
      </div>
    );
  }
);

Card.displayName = 'Card';

export default Card;
